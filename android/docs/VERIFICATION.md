# Отчёт о проверке — VidFetch (Android)

Версия: **1.1.0** (versionCode 2), движок скачивания — реальный HTTP-загрузчик + Room.

## 1. Сборка

```
$ ./gradlew assembleDebug assembleRelease
BUILD SUCCESSFUL
app/build/outputs/apk/debug/app-debug.apk      15 550 КБ
app/build/outputs/apk/release/app-release.apk  10 128 КБ
```

`aapt dump badging` (release): `package app.vidfetch.mobile`, `versionCode 2`,
`versionName 1.1.0`, `sdkVersion 26`, `targetSdkVersion 34`, label `VidFetch`.

## 2. Тесты (`./gradlew testDebugUnitTest`) — 18 тестов, 0 failures

| Тест | Что проверяет | Результат |
|------|----------------|-----------|
| `RoomPersistenceTest` | запись сохраняется и читается **после перезапуска БД** | ✅ |
| `DownloaderTest` | **реальная загрузка** файла с HTTP-сервера (MockWebServer): файл на диске 200 000 байт + статус `DONE` в Room | ✅ |
| `AppLaunchTest` | `MainActivity` стартует **без краша** (onCreate → Compose setContent) | ✅ |
| `ScreenshotTest` (13) / `ScreenshotTabletTest` (2) | рендер всех экранов/состояний (phone + tablet, light + dark) | ✅ |

## 3. Скриншоты (UI-презентация)

15 headless-снимков (Paparazzi) в `app/src/test/snapshots/images/`: главная (idle/loading/ready/error),
загрузки (content/empty/error), история (content/empty), настройки, тёмные варианты, планшет.

## 4. Данные и хранилище

Все списки экранов читаются из Room (`vidfetch.db`) через `Flow`; после завершения
загрузки запись сохраняется в БД и переживает перезапуск (см. `RoomPersistenceTest`).
Файлы кладутся в `getExternalFilesDir("downloads")`.

## 5. Ограничения

- **Эмулятор на этом хосте недоступен**: `emulator` требует включённых Hyper-V/WHPX
  (`x86_64 emulation currently requires hardware acceleration`). Поэтому установка/запуск
  на реальном устройстве здесь не выполнялись — вместо этого запуск активити, реальная
  загрузка и персистентность проверены юнит-тестами на JVM (Robolectric).
- Подпись — debug (release-ключ не предоставлен).
- YouTube-экстракция на устройстве не поддерживается (нужен yt-dlp); скачивание — по прямым ссылкам.
