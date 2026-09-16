'use strict';

/* ============================ VidFetch renderer ============================ */

const I18N = {
  ru: {
    settings: 'Настройки',
    paste: 'Вставить ссылку',
    analyze: 'Скачать',
    urlPlaceholder: 'Вставьте ссылку на видео или плейлист…',
    status: {
      ready: 'Готово',
      analyzing: 'Анализ ссылки…',
      downloading: 'Загрузка',
      done: 'Завершено',
      error: 'Ошибка',
    },
    downloads: 'Загрузки',
    empty: 'Вставьте ссылку, чтобы начать загрузку',
    dlOptions: {
      title: 'Параметры загрузки',
      mode: 'Режим',
      video: 'Видео',
      audio: 'Только аудио',
      format: 'Формат',
      quality: 'Качество',
      aquality: 'Качество аудио',
      best: 'Лучшее',
      folder: 'Папка назначения',
      browse: 'Обзор…',
      subtitles: 'Субтитры',
      sublangsPlaceholder: 'en, ru',
      metadata: 'Сохранить метаданные и обложку',
      cancel: 'Отмена',
      start: 'Начать загрузку',
    },
    settingsDlg: {
      title: 'Настройки',
      folder: 'Папка загрузок по умолчанию',
      concurrent: 'Одновременных загрузок',
      speedLimit: 'Ограничение скорости',
      unlimited: 'Без ограничений',
      clipboard: 'Автоматически добавлять ссылки из буфера обмена',
      after: 'После завершения загрузки',
      nothing: 'Ничего',
      openFolder: 'Открыть папку',
      theme: 'Тема',
      light: 'Светлая',
      dark: 'Тёмная',
      system: 'Системная',
      language: 'Язык интерфейса',
      done: 'Готово',
    },
    actions: {
      pause: 'Пауза',
      resume: 'Продолжить',
      cancel: 'Отмена',
      openFolder: 'Открыть папку',
      delete: 'Удалить',
      reveal: 'Показать в папке',
    },
    states: {
      queued: 'В очереди',
      downloading: 'Загрузка',
      postprocess: 'Обработка…',
      paused: 'Пауза',
      done: 'Завершено',
      error: 'Ошибка',
      cancelled: 'Отменено',
    },
    playlist: {
      title: 'Плейлист',
      items: '{n} видео',
      selectAll: 'Выбрать все',
      deselectAll: 'Снять выбор',
      downloadSelected: 'Скачать выбранные',
      downloadAll: 'Скачать всё',
      selected: 'Выбрано: {n}',
    },
    info: {
      download: 'Скачать',
      unavailable: 'Недоступно',
    },
    err: {
      badUrl: 'Не удалось распознать ссылку',
      engineDown: 'Движок загрузки недоступен',
      noSelection: 'Выберите хотя бы один элемент',
    },
    folder: 'Папка',
    videos: 'видео',
  },
  en: {
    settings: 'Settings',
    paste: 'Paste link',
    analyze: 'Download',
    urlPlaceholder: 'Paste a video or playlist link…',
    status: {
      ready: 'Ready',
      analyzing: 'Analyzing link…',
      downloading: 'Downloading',
      done: 'Completed',
      error: 'Error',
    },
    downloads: 'Downloads',
    empty: 'Paste a link to start downloading',
    dlOptions: {
      title: 'Download options',
      mode: 'Mode',
      video: 'Video',
      audio: 'Audio only',
      format: 'Format',
      quality: 'Quality',
      aquality: 'Audio quality',
      best: 'Best',
      folder: 'Destination folder',
      browse: 'Browse…',
      subtitles: 'Subtitles',
      sublangsPlaceholder: 'en, ru',
      metadata: 'Save metadata and thumbnail',
      cancel: 'Cancel',
      start: 'Start download',
    },
    settingsDlg: {
      title: 'Settings',
      folder: 'Default download folder',
      concurrent: 'Concurrent downloads',
      speedLimit: 'Speed limit',
      unlimited: 'Unlimited',
      clipboard: 'Auto-add links from clipboard',
      after: 'After download completes',
      nothing: 'Nothing',
      openFolder: 'Open folder',
      theme: 'Theme',
      light: 'Light',
      dark: 'Dark',
      system: 'System',
      language: 'Interface language',
      done: 'Done',
    },
    actions: {
      pause: 'Pause',
      resume: 'Resume',
      cancel: 'Cancel',
      openFolder: 'Open folder',
      delete: 'Delete',
      reveal: 'Show in folder',
    },
    states: {
      queued: 'Queued',
      downloading: 'Downloading',
      postprocess: 'Processing…',
      paused: 'Paused',
      done: 'Completed',
      error: 'Error',
      cancelled: 'Cancelled',
    },
    playlist: {
      title: 'Playlist',
      items: '{n} videos',
      selectAll: 'Select all',
      deselectAll: 'Deselect all',
      downloadSelected: 'Download selected',
      downloadAll: 'Download all',
      selected: 'Selected: {n}',
    },
    info: {
      download: 'Download',
      unavailable: 'Unavailable',
    },
    err: {
      badUrl: 'Could not recognize the link',
      engineDown: 'Download engine is unavailable',
      noSelection: 'Select at least one item',
    },
    folder: 'Folder',
    videos: 'videos',
  },
};

let LANG = 'ru';
function t(key) {
  let dict = I18N[LANG] || I18N.ru;
  const parts = key.split('.');
  let v = dict;
  for (const p of parts) {
    if (v == null) return key;
    v = v[p];
  }
  return typeof v === 'string' ? v : key;
}

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

let settings = {};
let currentTarget = null;   // { type: 'video', entry } | { type: 'playlist', url, title, entries }
let playlistSelection = []; // 0-based indices selected
let downloads = new Map();  // id -> { id, options, status, ... }
let engineAlive = false;
let lastClipboard = '';

/* ------------------------- helpers ------------------------- */

function fmtBytes(n) {
  if (n == null || isNaN(n)) return '—';
  const u = ['B', 'KB', 'MB', 'GB', 'TB'];
  let i = 0;
  while (n >= 1024 && i < u.length - 1) { n /= 1024; i++; }
  return `${n >= 100 ? Math.round(n) : n.toFixed(1)} ${u[i]}`;
}

function fmtDuration(s) {
  if (s == null || isNaN(s)) return '—';
  s = Math.round(s);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const sec = s % 60;
  const mm = String(m).padStart(2, '0');
  const ss = String(sec).padStart(2, '0');
  return h > 0 ? `${h}:${mm}:${ss}` : `${m}:${ss}`;
}

function fmtSpeed(bps) {
  if (bps == null || isNaN(bps)) return '';
  return `${fmtBytes(bps)}/s`;
}

function fmtEta(eta) {
  if (eta == null || isNaN(eta)) return '';
  return fmtDuration(eta);
}

function setStatus(state) {
  const pill = $('#status-pill');
  const txt = $('#status-text');
  pill.dataset.state = state;
  txt.textContent = t(`status.${state}`);
}

function show(el, on) {
  el.classList.toggle('hidden', !on);
}

/* ------------------------- settings / theme / i18n ------------------------- */

function applyTheme() {
  const mode = settings.theme || 'system';
  let resolved = mode;
  if (mode === 'system') {
    resolved = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  document.documentElement.dataset.theme = resolved;
}

function applyLanguage() {
  LANG = settings.language || 'ru';
  document.documentElement.lang = LANG;
  $$('[data-i18n]').forEach((el) => { el.textContent = t(el.dataset.i18n); });
  $$('[data-i18n-title]').forEach((el) => { el.title = t(el.dataset.i18nTitle); });
  $$('[data-i18n-placeholder]').forEach((el) => { el.placeholder = t(el.dataset.i18nPlaceholder); });
  rerenderPlaylistNote();
  rerenderDownloads();
}

function applySettings() {
  applyTheme();
  applyLanguage();
  // sync settings dialog controls
  setSeg('#seg-concurrent', String(settings.maxConcurrent));
  setSeg('#seg-speed', String(settings.speedLimit));
  setSeg('#seg-after', settings.afterDownload);
  setSeg('#seg-theme', settings.theme);
  setSeg('#seg-lang', settings.language);
  $('#set-folder').value = settings.downloadDir;
  $('#set-clipboard').checked = !!settings.clipboardWatch;
}

function setSeg(sel, value) {
  $$(`${sel} .seg`).forEach((b) => b.classList.toggle('active', b.dataset.value === value));
}

function segValue(sel) {
  const active = $(`${sel} .seg.active`);
  return active ? active.dataset.value : null;
}

function bindSeg(sel, cb) {
  $$(`${sel} .seg`).forEach((b) => {
    b.addEventListener('click', () => {
      $$(`${sel} .seg`).forEach((x) => x.classList.remove('active'));
      b.classList.add('active');
      if (cb) cb(b.dataset.value);
    });
  });
}

/* ------------------------- engine ------------------------- */

async function engineCall(method, params) {
  return window.vidfetch.call(method, params);
}

async function doAnalyze(url, autoStart) {
  url = (url || '').trim();
  if (!url) return;
  setStatus('analyzing');
  try {
    const res = await engineCall('analyze', { url });
    res.url = url;
    currentTarget = res;
    playlistSelection = [];
    updateEmptyState();
    if (res.type === 'video') {
      renderVideoInfo(res.entry);
      setStatus('ready');
      if (autoStart) openOptionsDialog();
    } else if (res.type === 'playlist') {
      renderPlaylist(res);
      setStatus('ready');
    }
  } catch (e) {
    setStatus('error');
    renderError(e);
  }
}

function renderError(e) {
  const info = $('#info-section');
  info.classList.remove('hidden');
  info.innerHTML = '';
  const div = document.createElement('div');
  div.className = 'info-meta';
  const msg = (e && e.message) || t('err.badUrl');
  div.innerHTML = `<div class="info-title" style="color:var(--err)">${escapeHtml(msg)}</div>`;
  info.appendChild(div);
}

function escapeHtml(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function thumbHtml(url, cls) {
  if (url) return `<img class="${cls}" src="${escapeHtml(url)}" onerror="this.classList.add('placeholder');this.removeAttribute('src');">`;
  return `<div class="${cls} placeholder">▶</div>`;
}

function renderVideoInfo(entry) {
  const info = $('#info-section');
  info.classList.remove('hidden');
  const heights = (entry.available_heights || []).slice(0, 6).map((h) => `${h}p`).join(' · ');
  info.innerHTML = `
    <div class="info-main">
      ${thumbHtml(entry.thumbnail, 'info-thumb')}
      <div class="info-meta">
        <div class="info-title">${escapeHtml(entry.title)}</div>
        <div class="info-sub">
          ${entry.uploader ? `<span>${escapeHtml(entry.uploader)}</span>` : ''}
          ${entry.duration ? `<span>${fmtDuration(entry.duration)}</span>` : ''}
          ${heights ? `<span class="info-badge">${escapeHtml(heights)}</span>` : ''}
        </div>
      </div>
    </div>
    <div class="info-actions">
      <button class="btn btn-primary btn-sm" id="btn-info-download">${t('info.download')}</button>
    </div>
  `;
  $('#btn-info-download').addEventListener('click', () => openOptionsDialog());
}

function renderPlaylist(pl) {
  const info = $('#info-section');
  info.classList.remove('hidden');
  const entries = pl.entries || [];
  playlistSelection = entries.map((_, i) => i); // default: all selected
  info.innerHTML = `
    <div class="playlist-head">
      <div>
        <div class="playlist-title">${t('playlist.title')}: ${escapeHtml(pl.title)}</div>
        <div class="playlist-count">${t('playlist.items').replace('{n}', entries.length)}</div>
      </div>
      <div class="info-actions">
        <button class="btn btn-secondary btn-sm" id="btn-pl-select">${t('playlist.deselectAll')}</button>
        <button class="btn btn-primary btn-sm" id="btn-pl-download">${t('playlist.downloadSelected')}</button>
      </div>
    </div>
    <div class="playlist" id="playlist-list">
      ${entries.map((e, i) => `
        <label class="pl-item checked" data-idx="${i}">
          <input type="checkbox" class="pl-check" data-idx="${i}" checked>
          <span class="pl-index">${i + 1}</span>
          ${thumbHtml(e.thumbnail, 'pl-thumb')}
          <span class="pl-name">${escapeHtml(e.title)}</span>
          <span class="pl-dur">${e.duration ? fmtDuration(e.duration) : ''}</span>
        </label>`).join('')}
    </div>
  `;

  $$('#playlist-list .pl-check').forEach((cb) => {
    cb.addEventListener('change', () => {
      const i = Number(cb.dataset.idx);
      if (cb.checked) { if (!playlistSelection.includes(i)) playlistSelection.push(i); }
      else playlistSelection = playlistSelection.filter((x) => x !== i);
      const label = cb.closest('.pl-item');
      label.classList.toggle('checked', cb.checked);
      rerenderPlaylistNote();
    });
  });

  $('#btn-pl-select').addEventListener('click', () => {
    const allSelected = playlistSelection.length === entries.length;
    if (allSelected) {
      playlistSelection = [];
      $$('#playlist-list .pl-check').forEach((cb) => { cb.checked = false; cb.closest('.pl-item').classList.remove('checked'); });
      $('#btn-pl-select').textContent = t('playlist.selectAll');
    } else {
      playlistSelection = entries.map((_, i) => i);
      $$('#playlist-list .pl-check').forEach((cb) => { cb.checked = true; cb.closest('.pl-item').classList.add('checked'); });
      $('#btn-pl-select').textContent = t('playlist.deselectAll');
    }
    rerenderPlaylistNote();
  });

  $('#btn-pl-download').addEventListener('click', () => {
    if (playlistSelection.length === 0) { setStatus('error'); renderError(new Error(t('err.noSelection'))); return; }
    openOptionsDialog();
  });
  rerenderPlaylistNote();
}

function rerenderPlaylistNote() {
  const note = $('#playlist-note');
  if (!currentTarget || currentTarget.type !== 'playlist') { show(note, false); return; }
  const n = playlistSelection.length;
  const total = currentTarget.entries.length;
  const selNote = n === total ? t('playlist.downloadAll') : t('playlist.selected').replace('{n}', n);
  show(note, true);
  note.textContent = `${t('playlist.title')}: ${currentTarget.title} — ${selNote}`;
  const btn = $('#btn-pl-download');
  if (btn) btn.textContent = n === total ? t('playlist.downloadAll') : t('playlist.downloadSelected');
}

/* ------------------------- options dialog ------------------------- */

function openOptionsDialog() {
  if (!currentTarget) return;
  const target = $('#dl-target');
  if (currentTarget.type === 'video') {
    const e = currentTarget.entry;
    target.innerHTML = `${thumbHtml(e.thumbnail, '')}<span class="t">${escapeHtml(e.title)}</span>`;
    // disable qualities not available
    const avail = e.available_heights || [];
    $$('#seg-quality .seg').forEach((b) => {
      b.disabled = avail.length > 0 && !avail.includes(Number(b.dataset.value));
    });
    // ensure selected quality is valid
    const selQ = Number(segValue('#seg-quality'));
    if (avail.length && !avail.includes(selQ)) {
      const best = avail[0];
      $$('#seg-quality .seg').forEach((b) => b.classList.toggle('active', Number(b.dataset.value) === best));
    }
  } else {
    target.innerHTML = `<span class="t">${t('playlist.title')}: ${escapeHtml(currentTarget.title)}</span>`;
    $$('#seg-quality .seg').forEach((b) => { b.disabled = false; });
  }
  $('#dl-folder').value = settings.downloadDir;
  show($('#dl-dialog'), true);
}

function currentDownloadOptions() {
  const mode = segValue('#seg-mode');
  const audioOnly = mode === 'audio';
  const o = {
    url: currentTarget.url,
    out_dir: $('#dl-folder').value || settings.downloadDir,
    audio_only: audioOnly,
    subtitles: $('#dl-subtitles').checked,
    subtitle_langs: parseLangs($('#dl-sublangs').value),
    embed_metadata: $('#dl-meta').checked,
    embed_thumbnail: $('#dl-meta').checked,
  };
  if (audioOnly) {
    o.audio_format = segValue('#seg-aformat');
    o.audio_quality = segValue('#seg-aquality');
  } else {
    o.format = segValue('#seg-format');
    o.quality = Number(segValue('#seg-quality'));
  }
  if (settings.speedLimit > 0) {
    o.ratelimit = settings.speedLimit * 1024 * 1024;
  }
  if (currentTarget.type === 'playlist') {
    const total = currentTarget.entries.length;
    if (playlistSelection.length < total) {
      o.playlist_items = playlistSelection.map((i) => i + 1);
    }
  }
  return o;
}

function urlFromTarget() {
  const e = currentTarget.entry;
  if (e.webpage_url) return e.webpage_url;
  if (e.id) return `https://www.youtube.com/watch?v=${e.id}`;
  return currentTarget.url || '';
}

function parseLangs(s) {
  return (s || '').split(',').map((x) => x.trim()).filter(Boolean);
}

/* ------------------------- downloads rendering ------------------------- */

function addDownloadCard(id, options) {
  downloads.set(id, { id, options, status: 'downloading', title: '', percent: null, speed: null, eta: null, downloaded: 0, total: 0, filepath: null, error: null });
  rerenderDownloads();
}

function updateEmptyState() {
  show($('#empty-state'), downloads.size === 0 && !currentTarget);
}

function rerenderDownloads() {
  const list = $('#downloads-list');
  const items = Array.from(downloads.values());
  list.innerHTML = '';
  updateEmptyState();

  items.forEach((d) => {
    const card = document.createElement('div');
    card.className = 'dl-card';
    card.dataset.id = d.id;
    card.dataset.state = d.status;
    const pct = d.percent == null ? '—' : `${Math.round(d.percent)}%`;
    const stats = [fmtBytes(d.downloaded)];
    if (d.total) stats.push(`/ ${fmtBytes(d.total)}`);
    const dur = d.options && d.options.duration ? fmtDuration(d.options.duration) : '';
    const title = d.title || d.options?.title || t('states.downloading');

    let stateText = t(`states.${d.status}`) || d.status;
    let bottom = '';
    if (d.status === 'downloading') {
      bottom = `<span class="dl-speed">${fmtSpeed(d.speed)}</span>
                <span class="dl-status-text">· ${fmtEta(d.eta)}${d.eta ? ' ' + t('states.postprocess').replace('…','') : ''}</span>`;
    } else if (d.status === 'error') {
      bottom = `<span class="dl-error">${escapeHtml(d.error || t('states.error'))}</span>`;
    } else if (d.status === 'done') {
      bottom = `<span class="dl-status-text">${d.filepath ? escapeHtml(d.filepath) : ''}</span>`;
    }

    card.innerHTML = `
      ${thumbHtml(d.options && d.options.thumbnail, 'dl-thumb')}
      <div class="dl-body">
        <div class="dl-top">
          <span class="dl-name" title="${escapeHtml(title)}">${escapeHtml(title)}</span>
          <span class="dl-stats">${dur}${dur ? ' · ' : ''}${stats.join(' ')}</span>
        </div>
        <div class="dl-progress"><div class="dl-bar" style="width:${d.percent == null ? 100 : d.percent}%"></div></div>
        <div class="dl-bottom">
          <span class="dl-percent">${pct}</span>
          ${bottom}
          <span class="dl-status-text" style="margin-left:auto">${escapeHtml(stateText)}</span>
        </div>
      </div>
      <div class="dl-actions">
        ${actionButton('pause', d, d.status === 'downloading')}
        ${actionButton('resume', d, d.status === 'paused')}
        ${actionButton('cancel', d, d.status === 'downloading' || d.status === 'paused' || d.status === 'queued')}
        ${actionButton('openFolder', d, !!d.filepath, true)}
        ${actionButton('delete', d, true, false, true)}
      </div>
    `;

    card.querySelectorAll('[data-act]').forEach((b) => {
      b.addEventListener('click', () => handleAction(b.dataset.act, d.id));
    });

    list.appendChild(card);
  });
}

function actionButton(act, d, enabled, isOpen = false, danger = false) {
  if (!enabled) return '';
  const icons = {
    pause: '<path d="M8 5v14M16 5v14"/>',
    resume: '<path d="M8 5l10 7-10 7z" fill="currentColor" stroke="none"/>',
    cancel: '<path d="M6 6l12 12M18 6L6 18"/>',
    openFolder: '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
    delete: '<path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13"/>',
  };
  const title = isOpen ? t('actions.openFolder') : t(`actions.${act}`);
  return `<button class="icon-btn ${danger ? 'danger' : ''}" data-act="${act}" title="${title}">
    <svg viewBox="0 0 24 24" width="16" height="16">${icons[act]}</svg>
  </button>`;
}

async function handleAction(act, id) {
  const d = downloads.get(id);
  if (!d) return;
  try {
    if (act === 'pause') { await engineCall('pause', { download_id: id }); d.status = 'paused'; }
    else if (act === 'resume') { await engineCall('resume', { download_id: id }); d.status = 'downloading'; d.percent = d.percent || 0; }
    else if (act === 'cancel') { await engineCall('cancel', { download_id: id }); d.status = 'cancelled'; }
    else if (act === 'openFolder') { window.vidfetch.reveal(d.filepath || settings.downloadDir); }
    else if (act === 'delete') { await engineCall('cancel', { download_id: id }).catch(() => {}); downloads.delete(id); }
  } catch (e) {
    console.error('action failed', act, e);
  }
  rerenderDownloads();
}

/* ------------------------- engine events ------------------------- */

function onEngineEvent(msg) {
  const ev = msg.event;
  const data = msg.data || {};
  const id = data.download_id;
  if (!id) return;

  if (ev === 'progress') {
    const d = downloads.get(id);
    if (!d) return;
    if (data.status === 'postprocess') { d.status = 'postprocess'; }
    else {
      d.status = 'downloading';
      d.percent = data.percent;
      d.speed = data.speed;
      d.eta = data.eta;
      d.downloaded = data.downloaded_bytes;
      d.total = data.total_bytes;
      if (data.title) d.title = data.title;
    }
    setStatus('downloading');
    rerenderDownloads();
  } else if (ev === 'done') {
    const d = downloads.get(id);
    if (!d) return;
    d.status = 'done';
    d.filepath = data.filepath;
    d.title = data.title || d.title;
    d.percent = 100;
    setStatus('done');
    rerenderDownloads();
    if (settings.afterDownload === 'openFolder' && d.filepath) {
      window.vidfetch.reveal(d.filepath);
    }
  } else if (ev === 'cancelled') {
    const d = downloads.get(id);
    if (!d) return;
    d.status = data.paused ? 'paused' : 'cancelled';
    setStatus('ready');
    rerenderDownloads();
  } else if (ev === 'error') {
    const d = downloads.get(id);
    if (!d) return;
    d.status = 'error';
    d.error = data.error;
    setStatus('error');
    rerenderDownloads();
  }
}

/* ------------------------- clipboard watch ------------------------- */

function looksLikeUrl(s) {
  return /^(https?:\/\/|www\.)/i.test(s) && !/\s/.test(s);
}

async function clipboardTick() {
  if (!settings.clipboardWatch) return;
  const txt = await window.vidfetch.readClipboard();
  if (txt && txt !== lastClipboard && looksLikeUrl(txt)) {
    lastClipboard = txt;
    const input = $('#url-input');
    if (input.value !== txt) {
      input.value = txt;
      doAnalyze(txt, false);
    }
  }
}

/* ------------------------- init ------------------------- */

function bindStatic() {
  $('#btn-analyze').addEventListener('click', () => doAnalyze($('#url-input').value, true));
  $('#btn-paste').addEventListener('click', async () => {
    const txt = await window.vidfetch.readClipboard();
    if (txt) { $('#url-input').value = txt; doAnalyze(txt, true); }
  });
  $('#url-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') doAnalyze(e.target.value, true);
  });

  $('#btn-settings').addEventListener('click', () => { applySettings(); show($('#settings-dialog'), true); });
  $('#btn-min').addEventListener('click', () => window.vidfetch.minimize());
  $('#btn-max').addEventListener('click', () => window.vidfetch.maximize());
  $('#btn-close').addEventListener('click', () => window.vidfetch.close());

  // modal close buttons
  $$('[data-close]').forEach((b) => b.addEventListener('click', () => show($(`#${b.dataset.close}`), false)));
  $$('.modal-backdrop').forEach((m) => m.addEventListener('click', (e) => {
    if (e.target === m) m.classList.add('hidden');
  }));

  // options dialog
  bindSeg('#seg-mode', (v) => {
    show($('#video-opts'), v === 'video');
    show($('#audio-opts'), v === 'audio');
  });
  bindSeg('#seg-format', () => {});
  bindSeg('#seg-quality', () => {});
  bindSeg('#seg-aformat', () => {});
  bindSeg('#seg-aquality', () => {});
  $('#btn-browse').addEventListener('click', async () => {
    const p = await window.vidfetch.pickFolder($('#dl-folder').value);
    if (p) $('#dl-folder').value = p;
  });
  $('#btn-start-download').addEventListener('click', startDownloadFromDialog);

  // settings dialog
  $('#btn-set-browse').addEventListener('click', async () => {
    const p = await window.vidfetch.pickFolder($('#set-folder').value);
    if (p) $('#set-folder').value = p;
  });
  bindSeg('#seg-concurrent', () => {});
  bindSeg('#seg-speed', () => {});
  bindSeg('#seg-after', () => {});
  bindSeg('#seg-theme', () => {});
  bindSeg('#seg-lang', () => {});
  $('#settings-dialog [data-close]').addEventListener('click', saveSettingsFromDialog);
}

async function startDownloadFromDialog() {
  const o = currentDownloadOptions();
  if (!o.url) return;
  show($('#dl-dialog'), false);
  try {
    const res = await engineCall('download', { options: o });
    addDownloadCard(res.download_id, Object.assign({}, o, {
      title: currentTarget.type === 'video' ? currentTarget.entry.title : currentTarget.title,
      thumbnail: currentTarget.type === 'video' ? currentTarget.entry.thumbnail : null,
      duration: currentTarget.type === 'video' ? currentTarget.entry.duration : null,
    }));
    setStatus('downloading');
  } catch (e) {
    setStatus('error');
    renderError(e);
  }
}

async function saveSettingsFromDialog() {
  const patch = {
    downloadDir: $('#set-folder').value || settings.downloadDir,
    maxConcurrent: Number(segValue('#seg-concurrent') || 2),
    speedLimit: Number(segValue('#seg-speed') || 0),
    afterDownload: segValue('#seg-after') || 'nothing',
    theme: segValue('#seg-theme') || 'system',
    language: segValue('#seg-lang') || 'ru',
    clipboardWatch: $('#set-clipboard').checked,
  };
  settings = await window.vidfetch.setSettings(patch);
  applySettings();
}

async function init() {
  bindStatic();
  settings = await window.vidfetch.getSettings();
  applySettings();

  window.vidfetch.onEvent(onEngineEvent);
  window.vidfetch.onStatus((s) => { engineAlive = !!s.alive; });
  window.vidfetch.onMaximized((v) => {
    const b = $('#btn-max');
    b.innerHTML = v
      ? '<svg viewBox="0 0 24 24" width="16" height="16"><path d="M8 4h12v12M4 8v12h12"/></svg>'
      : '<svg viewBox="0 0 24 24" width="16" height="16"><rect x="6" y="6" width="12" height="12" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.8"/></svg>';
  });

  // restore maximized state
  const isMax = await window.vidfetch.isMaximized();
  if (isMax) {
    $('#btn-max').innerHTML = '<svg viewBox="0 0 24 24" width="16" height="16"><path d="M8 4h12v12M4 8v12h12"/></svg>';
  }

  // poll clipboard (light)
  setInterval(clipboardTick, 1600);
}

// dev/test hooks (used by screenshot mode)
window.__vf = {
  setUrl: (u) => { $('#url-input').value = u; },
  analyze: (u) => doAnalyze(u, false),
  openOptions: () => openOptionsDialog(),
  openSettings: () => { applySettings(); show($('#settings-dialog'), true); },
};

init();
