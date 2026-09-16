#!/usr/bin/env python3
"""
VidFetch download engine.

A UI-agnostic backend that speaks a JSON-lines protocol over stdin/stdout.
It is intentionally decoupled from any UI so it can be replaced by another
backend (native, remote, etc.) without touching the interface.

Protocol
--------
Request (one JSON line):
    {"id": <int>, "method": "<name>", "params": { ... }}

Response (one JSON line):
    {"id": <int>, "ok": true, "result": { ... }}
    {"id": <int>, "ok": false, "error": "<message>"}

Unsolicited events (one JSON line):
    {"event": "progress" | "done" | "error" | "cancelled", "data": { ... }}

Methods: ping, analyze, download, pause, resume, cancel
"""

import json
import os
import sys
import threading

try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")
except Exception:
    pass

try:
    import yt_dlp
    from yt_dlp.utils import DownloadCancelled
except Exception as _e:  # pragma: no cover - defensive
    yt_dlp = None
    DownloadCancelled = Exception
    _IMPORT_ERR = str(_e)

_WRITE_LOCK = threading.Lock()
_ACTIVE = {}          # download_id -> control dict
_NEXT_ID = 1
_ID_LOCK = threading.Lock()

VERSION = "1.0.1"


def _ytdlp_version():
    try:
        return yt_dlp.version.__version__
    except Exception:
        return None


def _ffmpeg_location():
    """Return bundled ffmpeg path when present."""
    base = os.environ.get("VIDFETCH_FFMPEG_DIR", "")
    if not base:
        base = os.path.dirname(os.path.abspath(__file__))
    candidates = (
        [os.path.join(base, "ffmpeg.exe"), os.path.join(base, "ffmpeg")]
        if os.name == "nt"
        else [os.path.join(base, "ffmpeg")]
    )
    for c in candidates:
        if os.path.exists(c):
            return c
    parent = os.path.join(os.path.dirname(base), "ffmpeg")
    exe = parent + (".exe" if os.name == "nt" else "")
    if os.path.exists(exe):
        return exe
    return None


def _emit(obj):
    with _WRITE_LOCK:
        sys.stdout.write(json.dumps(obj, ensure_ascii=False) + "\n")
        sys.stdout.flush()


def _send_ok(req_id, result):
    _emit({"id": req_id, "ok": True, "result": result})


def _send_err(req_id, msg):
    _emit({"id": req_id, "ok": False, "error": str(msg)})


# --------------------------------------------------------------------------- #
# Format / option builders
# --------------------------------------------------------------------------- #

def _format_spec(o):
    if o.get("audio_only"):
        return "bestaudio/best"
    q = o.get("quality")
    if q:
        return f"bestvideo[height<={q}]+bestaudio/best[height<={q}]"
    return "bestvideo+bestaudio/best"


def _build_opts(o):
    loc = _ffmpeg_location()
    opts = {
        "quiet": True,
        "no_warnings": True,
        "noprogress": True,
        "noplaylist": False,
        "continuedl": True,
        "retries": 10,
        "fragment_retries": 10,
        "overwrites": False,
        "windowsfilenames": False,
        "ffmpeg_location": loc,
        "outtmpl": os.path.join(o["out_dir"], "%(title).180B [%(id)s].%(ext)s"),
    }
    fmt = _format_spec(o)
    opts["format"] = fmt
    if not o.get("audio_only") and o.get("format") in ("mp4", "mkv", "webm"):
        opts["merge_output_format"] = o["format"]

    if o.get("ratelimit"):
        opts["ratelimit"] = int(o["ratelimit"])

    if o.get("playlist_items"):
        opts["playlist_items"] = ",".join(str(int(i)) for i in o["playlist_items"])

    # Subtitles
    if o.get("subtitles"):
        opts["writesubtitles"] = True
        opts["writeautomaticsub"] = True
        opts["subtitlesformat"] = "best/srt"
        opts["subtitleslangs"] = o.get("subtitle_langs") or ["en"]

    # Metadata / thumbnail
    if o.get("embed_metadata"):
        opts["embedmetadata"] = True
    if o.get("embed_thumbnail"):
        opts["writethumbnail"] = True
        opts["embedthumbnail"] = True

    # Audio extraction
    if o.get("audio_only"):
        pp = {
            "key": "FFmpegExtractAudio",
            "preferredcodec": o.get("audio_format") or "mp3",
        }
        aq = o.get("audio_quality")
        if aq and str(aq) != "best" and pp["preferredcodec"] != "wav":
            pp["preferredquality"] = str(aq)
        opts["postprocessors"] = [pp]

    return opts


# --------------------------------------------------------------------------- #
# Analyze
# --------------------------------------------------------------------------- #

def _entry(info):
    heights = sorted(
        {f.get("height") for f in info.get("formats", []) if f.get("height")},
        reverse=True,
    )
    exts = sorted({f.get("ext") for f in info.get("formats", []) if f.get("ext")})
    return {
        "id": info.get("id"),
        "title": info.get("title"),
        "duration": info.get("duration"),
        "thumbnail": info.get("thumbnail"),
        "uploader": info.get("uploader") or info.get("channel"),
        "webpage_url": info.get("webpage_url"),
        "height": info.get("height"),
        "width": info.get("width"),
        "available_heights": heights,
        "formats_ext": exts,
    }


def analyze(url):
    if yt_dlp is None:
        raise RuntimeError(f"yt-dlp unavailable: {_IMPORT_ERR}")
    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": False,
        "extract_flat": "in_playlist",
        "ffmpeg_location": _ffmpeg_location(),
    }
    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=False)
    if info is None:
        raise RuntimeError("no data returned")

    if info.get("_type") == "playlist" or "entries" in info:
        entries = []
        for e in info.get("entries") or []:
            if not e:
                continue
            entries.append(
                {
                    "id": e.get("id"),
                    "title": e.get("title"),
                    "duration": e.get("duration"),
                    "thumbnail": e.get("thumbnail"),
                }
            )
        return {
            "type": "playlist",
            "title": info.get("title"),
            "entries": entries,
        }

    return {"type": "video", "entry": _entry(info)}


# --------------------------------------------------------------------------- #
# Download workers
# --------------------------------------------------------------------------- #

def _download_worker(did, o, ctl):
    try:
        opts = _build_opts(o)

        def progress_hook(d):
            if ctl["cancel"]:
                raise DownloadCancelled("cancelled by user")
            if d.get("status") == "downloading":
                ctl["current_file"] = d.get("filename")
                total = d.get("total_bytes") or d.get("total_bytes_estimate") or 0
                downloaded = d.get("downloaded_bytes", 0)
                percent = (downloaded / total * 100.0) if total else None
                info = d.get("info_dict") or {}
                ctl["title"] = info.get("title")
                _emit({
                    "event": "progress",
                    "data": {
                        "download_id": did,
                        "status": "downloading",
                        "percent": percent,
                        "downloaded_bytes": downloaded,
                        "total_bytes": total,
                        "speed": d.get("speed"),
                        "eta": d.get("eta"),
                        "title": info.get("title"),
                        "playlist_index": info.get("playlist_index"),
                        "playlist_count": info.get("playlist_count"),
                    },
                })
            elif d.get("status") == "finished":
                ctl["current_file"] = d.get("filename")
                ctl["title"] = (d.get("info_dict") or {}).get("title")
                _emit({
                    "event": "progress",
                    "data": {
                        "download_id": did,
                        "status": "postprocess",
                        "title": ctl["title"],
                    },
                })

        def postprocess_hook(d):
            if d.get("status") == "finished":
                fp = (d.get("info_dict") or {}).get("filepath")
                if fp:
                    ctl["final_filepath"] = fp

        opts["progress_hooks"] = [progress_hook]
        opts["postprocessor_hooks"] = [postprocess_hook]

        with yt_dlp.YoutubeDL(opts) as ydl:
            ctl["ydl"] = ydl
            ydl.download([o["url"]])

        if ctl["cancel"]:
            _cleanup_partial(ctl)
            _emit({"event": "cancelled", "data": {"download_id": did, "paused": ctl.get("paused", False)}})
            return

        fp = ctl.get("final_filepath") or ctl.get("current_file")
        _emit({
            "event": "done",
            "data": {
                "download_id": did,
                "filepath": fp,
                "title": ctl.get("title"),
            },
        })
    except DownloadCancelled:
        if ctl.get("paused"):
            _emit({"event": "cancelled", "data": {"download_id": did, "paused": True}})
        else:
            _cleanup_partial(ctl)
            _emit({"event": "cancelled", "data": {"download_id": did, "paused": False}})
    except Exception as e:
        if ctl["cancel"]:
            _cleanup_partial(ctl)
            _emit({"event": "cancelled", "data": {"download_id": did, "paused": ctl.get("paused", False)}})
        else:
            _emit({"event": "error", "data": {"download_id": did, "error": str(e)}})
    finally:
        with _ID_LOCK:
            _ACTIVE.pop(did, None)


def _cleanup_partial(ctl):
    cf = ctl.get("current_file")
    if cf:
        for suffix in (".part", ".ytdl"):
            cand = cf + suffix if not cf.endswith(suffix) else cf
            try:
                if os.path.exists(cand):
                    os.remove(cand)
            except Exception:
                pass


def _start_worker(did, o):
    ctl = {"cancel": False, "paused": False, "current_file": None, "final_filepath": None,
           "title": None, "ydl": None, "options": o}
    _ACTIVE[did] = ctl
    t = threading.Thread(target=_download_worker, args=(did, o, ctl), daemon=True)
    ctl["thread"] = t
    t.start()


def start_download(o):
    global _NEXT_ID
    with _ID_LOCK:
        did = _NEXT_ID
        _NEXT_ID += 1
    _start_worker(did, o)
    return {"download_id": did}


def pause(did):
    ctl = _ACTIVE.get(did)
    if not ctl:
        raise RuntimeError("unknown download id")
    ctl["paused"] = True
    ctl["cancel"] = True
    return {"download_id": did}


def resume(did):
    ctl = _ACTIVE.get(did)
    if not ctl:
        raise RuntimeError("unknown download id")
    ctl["cancel"] = False
    ctl["paused"] = False
    _start_worker(did, ctl["options"])
    return {"download_id": did}


def cancel(did):
    ctl = _ACTIVE.get(did)
    if not ctl:
        raise RuntimeError("unknown download id")
    ctl["paused"] = False
    ctl["cancel"] = True
    return {"download_id": did}


# --------------------------------------------------------------------------- #
# Main loop
# --------------------------------------------------------------------------- #

METHODS = {
    "ping": lambda p: {"pong": True, "version": VERSION, "ytdlp": _ytdlp_version(), "ffmpeg": bool(_ffmpeg_location())},
    "analyze": lambda p: analyze(p["url"]),
    "download": lambda p: start_download(p["options"]),
    "pause": lambda p: pause(p["download_id"]),
    "resume": lambda p: resume(p["download_id"]),
    "cancel": lambda p: cancel(p["download_id"]),
}


def main():
    _emit({"event": "ready", "data": {"version": VERSION, "ytdlp": _ytdlp_version(), "ffmpeg": bool(_ffmpeg_location())}})
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
        except Exception as e:
            _emit({"id": None, "ok": False, "error": f"bad request: {e}"})
            continue
        rid = req.get("id")
        method = req.get("method")
        params = req.get("params") or {}
        if method not in METHODS:
            _send_err(rid, f"unknown method: {method}")
            continue
        try:
            _send_ok(rid, METHODS[method](params))
        except Exception as e:
            _send_err(rid, str(e))


if __name__ == "__main__":
    main()
