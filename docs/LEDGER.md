# Журнал исполнения (execution ledger)

Датированный журнал ключевых решений, изменений и вех. Ведётся в ходе поставки.

## 2026-09-16

- **[решение] Стек.** Electron (Node 22 + npm, реестр отвечает 200) для UI; Python 3.13
  + yt-dlp 2025.12.08 для движка; ffmpeg бандлится отдельно (в системе отсутствовал).
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
- **[веха M1] Исходники.** `engine/`, `electron/`, `renderer/`, `tests/`, иконки, `package.json`.
- **[исправление]** У single-video analyse не было исходного URL → `res.url = url` +
  `webpage_url` в `_entry`.
- **[веха M2] Движок проверен.** `ping` (ffmpeg=true), `analyze` (реальные видео: «Me at the
  zoo» 19 с/240p; «Rick Astley 4K Remaster» до 2160p), `download` (аудио→MP3, 9 progress-
  событий, файл 304 748 байт + обложка .webp, событие `done`). Тест: `tests/smoke_test.py`.
- **[веха M3] UI визуально проверен.** Скриншоты + vision QA (qwen3.8-27b): «современное
  коммерческое desktop-приложение». Найдено и исправлено: пустая подсказка оставалась
  после анализа (fix: `updateEmptyState()` по `currentTarget`) — повторный QA подтвердил.
- **[веха M4] Сборки.**
  - Windows: PyInstaller → `engine.exe` (15.38 МБ), ffmpeg 9.0.1 (98 МБ). `electron-builder
    --win` → NSIS + portable (по ~119 МБ). Движок и ffmpeg забандлены (проверено в `resources/`).
  - Linux: движок заморожен в WSL (PyInstaller 6.22.3, yt-dlp 2026.08.19) → ELF64 (24.46 МБ);
    статический ffmpeg 7.0.2 (76 МБ); `electron-builder --linux` собрал `release/linux-unpacked/`
    (полный раннинг-каталог). AppImage/deb не собраны: `mksquashfs`/`fpm` — Linux-бинарики,
    а в WSL нет Node (см. HANDOVER).
- **[веха M5] Публикация.** Репозиторий `https://github.com/larin-ilya/vidfetch` (public,
  ветка `main`, 17 файлов). Релиз `v1.0.0`:
  - `VidFetch-1.0.0-portable-win-x64.exe`
  - `VidFetch-1.0.0-setup-win-x64.exe`
  (SHA256 в теле релиза: `https://github.com/larin-ilya/vidfetch/releases/tag/v1.0.0`).
- **[веха M6] Передача.** README / PLAN / LEDGER / HANDOVER готовы.

## 2026-09-16 (fix)

- **[исправление] yt-dlp nightly.** Стабильная `2025.12.08` устарела — YouTube не качал.
  Установлен nightly `2026.08.30.232658` (`pip install -U --pre yt-dlp`), движок перезаморожен
  и проверен; `ping`/`ready` теперь возвращают версию yt-dlp. Версия приложения 1.0.1,
  пересобраны Windows-бинарики, опубликован релиз v1.0.1.
- **[проверка] Реальный прогон релиза v1.0.1.** Упакованный `engine.exe` из `release/win-unpacked`
  выполнил полный цикл (ping → analyze → download) и скачал реальный файл (MP3 304 748 байт).
  Скачанный с GitHub артефакт `VidFetch-1.0.1-portable-win-x64.exe` совпал с локальным билдом
  (SHA256 `6baaded9…`, 164 МБ).
- **[проверка] Сквозной прогон релиза v1.0.1 через UI.** Опубликованный portable exe запущен с CDP-отладкой;
  через самый интерфейс задана ссылка → анализ → диалог параметров (Видео·MP4) → папка → старт.
  Реально скачан файл `Me at the zoo [jNQXAC9IVRw].mp4` (475 989 байт); UI: 0.0 B → 246 KB/246 KB, 100%,
  «Завершено». Итоговый экран подтверждён визуально.
