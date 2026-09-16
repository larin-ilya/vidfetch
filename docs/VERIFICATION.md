# Отчёт о проверке — VidFetch (desktop, Windows/Linux)

Дата: 2026-09-16. Версия: **1.0.1** (движок на **yt-dlp nightly `2026.08.30.232658`**).

Ниже — зафиксированные **реальные прогоны** (не визуальный осмотр), с наблюдаемыми результатами.

## 1. Опубликованный артефакт (биты, которые скачивает пользователь)

С GitHub скачан `VidFetch-1.0.1-portable-win-x64.exe` (164 МБ), распакован, и **его собственные**
`resources/engine/engine.exe` + `resources/ffmpeg/ffmpeg.exe` выполнили полную загрузку:

```
PING:    {"pong":true,"version":"1.0.1","ytdlp":"2026.08.30.232658","ffmpeg":true}
ANALYZE: video | Me at the zoo | heights: [240, 144]
DOWNLOAD: 9 progress-событий → done
FILE:    Me at the zoo [jNQXAC9IVRw].mp3 — 304 748 байт
```

**Вывод:** именно скачиваемые пользователем бинарники реально качают видео. ✅

## 2. Сквозной прогон через интерфейс релиза

Запущен опубликованный portable exe; сценарий выполнен **через UI**: ссылка → «Скачать» →
диалог параметров (режим «Видео», формат MP4) → папка → «Начать загрузку».

```
[0s] card: state=downloading  stats=0:19 · 0.0 B            pct=—
[1s] card: state=done         stats=0:19 · 246 KB / 246 KB  pct=100%
FILE: Me at the zoo [jNQXAC9IVRw].mp4 — 475 989 байт
```

Итоговый экран подтверждён визуально: зелёная полоса, «100 %», статус «Завершено», дефектов нет. ✅

## 3. Упакованный движок релизной сборки

`release/win-unpacked/resources/engine/engine.exe` (то, что кладётся в пакет): `ping` → nightly;
реальная загрузка → MP3 304 748 байт, событие `done`. ✅

## 4. Целостность артефакта

Скачанный с GitHub exe **байт-в-байт** совпал с локальной сборкой:

```
local  sha256 : 6baaded99802b59d27632d578e938bc15a2b08fa344f2a66108caf64d9084db9
github sha256 : 6baaded99802b59d27632d578e938bc15a2b08fa344f2a66108caf64d9084db9
```

## 5. Linux-движок

`engine/dist/engine` (ELF64 x86-64): `ping` → `ytdlp: 2026.08.30.232658`. ✅

## Воспроизведение

```bash
# движок (Windows)
set VIDFETCH_FFMPEG_DIR=<...>\build\ffmpeg
echo {"id":1,"method":"ping","params":{}} | engine\dist\engine.exe

# реальная загрузка
python tests\smoke_test.py
```

## Что осталось незакрытым

- Linux AppImage/deb: сборка требует Linux-хоста (`mksquashfs`/`fpm`). Движок и `release/linux-unpacked/` готовы.
- Production-подпись/публикация в стор — вне объёма.
