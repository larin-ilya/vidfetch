# Handover — VidFetch

> Дата: 2026-09-16. Статус: готово к приёмке.

## 1. Что доставлено

Полноценное desktop-приложение «VidFetch» (загрузчик видео/аудио) с исходным кодом
и собранными артефактами.

| Артефакт | Расположение |
|----------|--------------|
| Репозиторий (исходники) | https://github.com/larin-ilya/vidfetch |
| Релиз v1.0.0 (Windows x64) | https://github.com/larin-ilya/vidfetch/releases/tag/v1.0.0 |
| — portable exe | `VidFetch-1.0.0-portable-win-x64.exe` (~119 МБ) |
| — установщик NSIS | `VidFetch-1.0.0-setup-win-x64.exe` (~119 МБ) |
| Linux unpacked (раннинг) | `release/linux-unpacked/vidfetch` (собран, см. §4 п.6) |

## 2. Как пользоваться (Windows)

1. Скачать portable exe или установить через Setup.
2. Вставить ссылку (кнопка «Вставить ссылку» читает буфер обмена) или вручную.
3. Приложение определит видео или плейлист.
4. «Скачать» → выбрать формат/качество/папку → «Начать загрузку».
5. Прогресс → «Открыть папку» / «Показать в папке».

## 3. Как собрать из исходников

```bash
npm install
python -m pip install -r engine/requirements.txt
python -m pip install pyinstaller
python -m PyInstaller --onefile --name engine engine/engine.py   # -> engine/dist/engine[.exe]
# положить ffmpeg в build/ffmpeg/ (Windows: ffmpeg.exe; Linux: ffmpeg)
npm start                  # dev
npm run dist:win           # NSIS + portable
npm run dist:linux         # AppImage + deb (сборка на Linux-хосте)
```

## 4. Известные ограничения

1. **Android** не покрывается: Electron не работает на Android, а yt-dlp (Python) на
   устройстве требует отдельного порта (Chaquopy/python-for-android) — отдельный
   тулчейн, не входит в текущую поставку. Архитектура (движок отделён от UI) это
   допускает: движок можно поднять как локальный сервис и написать нативную оболочку.
2. **WebM** зависит от наличия VP9/Opus в источнике: если у выбранного качества их нет,
   yt-dlp вернёт ошибку мёрджа (показывается как «Ошибка»).
3. **Пауза/Продолжить** — через остановку процесса и докачку (`continuedl`); возобновление
   зависит от поддержки фрагментов/`Content-Range` источником.
4. **Скорость/ETA** для живых потоков может отсутствовать (неизвестный размер).
5. **YouTube ToS**: скачивание с YouTube может нарушать их условия; только для контента
   с правом на загрузку. Приложение не обходит DRM.
6. **Linux AppImage/deb** не собраны на этой машине: `mksquashfs`/`fpm` (инструменты
   electron-builder для AppImage/deb) — Linux-бинарики, а в WSL Debian нет Node. Однако
   движок (ELF64) и статический ffmpeg (7.0.2) уже собраны, и `release/linux-unpacked/`
   — полный запускаемый каталог Linux-сборки. Для AppImage/deb выполнить на Linux-хосте
   (или в WSL с Node): `npm install && npm run dist:linux`.

## 5. Чек-лист приёмки

- [x] `npm install` проходит без ошибок (проверено: 404 пакета).
- [x] Движок отвечает на `ping` (проверено: `{"pong":true,"ffmpeg":true}`).
- [x] `analyze` возвращает метаданные реального видео (проверено: «Me at the zoo» 19 с/240p;
      «Rick Astley 4K Remaster» до 2160p).
- [x] Реальная загрузка: аудио → MP3 304 748 байт + встроенная обложка (smoke test).
- [x] UI рендерится, визуально проверен (vision QA: «коммерческое desktop-приложение»).
- [x] Сборка Windows: NSIS + portable; движок и ffmpeg заморожены и забандлены.
- [x] Репозиторий запушен на GitHub; токен не раскрыт.
- [ ] (опционально) Linux AppImage/deb — собрать на Linux-хосте (см. §4 п.6).

## 6. Подпись

Приёмка подтверждается после прохождения всех пунктов §5 пользователем.
