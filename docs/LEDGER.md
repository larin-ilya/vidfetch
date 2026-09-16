# Журнал исполнения (execution ledger)

Датированный журнал ключевых решений, изменений и вех. Ведётся в ходе поставки.

## 2026-09-16

- **[решение] Стек.** Electron (Node 22 + npm, реестр отвечает 200) для UI; Python 3.13
  + yt-dlp 2025.12.08 для движка; ffmpeg бандлится отдельно (в системе отсутствует).
  Flutter/Rust/Java отсутствуют → не используются.
- **[решение] Архитектура движка.** `engine.py` общается JSON-lines поверх stdin/stdout
  (`ping/analyze/download/pause/resume/cancel`). Отмена/пауза — через `DownloadCancelled`
  из progress-hook (подтверждено по исходникам yt-dlp).
- **[решение] Разделение песочниц.** Файловые инструменты ограничены control-workspace;
  репозиторий привязан к `C:\Users\user\.openclaw-autoclaw\workspace`. Файлы стейджатся
  в `.openclaw/tmp/vidfetch/` и копируются в bound workspace.
- **[решение] GitHub.** Пользователь `larin-ilya` (через API по токену, ключ не выводился).
  Пуш через `GIT_ASKPASS`; токен не попадает в вывод и в репозиторий.
- **[решение] Визуальный стиль.** Пресет «11 Build» (luxury minimalism): 70%+ воздуха,
  один акцентный индиго `#5B5FE9`, мягкие тени, скругления, системная типографика.
- **[веха M1] Исходники.** `engine/`, `electron/`, `renderer/`, `package.json`, иконки.
- **[исправление]** У single-video analyse не было исходного URL → `res.url = url` +
  `webpage_url` в `_entry`.
- **[веха M2] Движок проверен.** `ping` (ffmpeg=true), `analyze` (реальные видео),
  `download` (аудио→MP3, 9 progress-событий, файл 304 КБ + обложка .webp, событие `done`).
- **[веха M3] UI визуально проверен.** Скриншоты + vision QA (qwen3.8-27b): «современное
  коммерческое desktop-приложение». Найдено и исправлено: пустая подсказка оставалась
  после анализа (fix: `updateEmptyState()` по `currentTarget`).
- **[веха M4] Сборка Windows.** PyInstaller заморозил движок (engine.exe 15.4 МБ),
  ffmpeg 9.0.1 забандлен (98 МБ). `electron-builder --win` → NSIS + portable (~119 МБ).
- **[веха M5] Публикация.** (заполняется)
- **[веха M6] Передача.** README/PLAN/LEDGER/HANDOVER (заполняется).
