# Отчёт о проверке — VidFetch (Android)

Версия: **1.1.0** (versionCode 2). Скачивание — реальный HTTP-загрузчик; хранение — Room.

## 1. Сборка

```
$ ./gradlew assembleDebug assembleRelease
BUILD SUCCESSFUL
app/build/outputs/apk/debug/app-debug.apk      15 631 КБ
app/build/outputs/apk/release/app-release.apk  10 130 КБ
```

`aapt dump badging` (release): `app.vidfetch.mobile`, `versionCode 2`, `versionName 1.1.0`,
`sdkVersion 26`, `targetSdkVersion 34`, label `VidFetch`.

## 2. Тесты (`./gradlew testDebugUnitTest`) — 22 теста, 0 failures

| Тест | Что проверяет | Итог |
|------|----------------|------|
| `RoomPersistenceTest` | запись сохраняется и читается **после перезапуска БД** | ✅ |
| `DownloaderTest` | **реальная загрузка** файла (MockWebServer): файл на диске 200 000 байт + `DONE` в Room | ✅ |
| `LinkAnalyzerTest` (3) | приём прямых ссылок; fallback **HEAD → GET+Range** при отказе (405); нормализация URL | ✅ |
| `ExportTest` | выгрузка журнала в CSV (заголовок + записи) | ✅ |
| `AppLaunchTest` | `MainActivity` стартует **без краша** | ✅ |
| `ScreenshotTest` (13) / `ScreenshotTabletTest` (2) | рендер всех экранов/состояний | ✅ |

## 3. Приём ссылок (исправлено)

Разбор ссылок устойчив: сначала `HEAD`; при отказе сервера (405/403 и т.п.) — `GET` c
`Range: bytes=0-0`; поддержка редиректов, `Content-Disposition` (имя файла),
`Content-Range` (полный размер), нормализация URL (добавляет `https://`). В интерфейсе —
**вставка из буфера**. Веб‑страницы (`text/html`) помечаются как «не файл», без мусорной загрузки.

## 4. Хранилище и журнал

- Все загрузки и их прогресс/статусы пишутся в Room (`vidfetch.db`); настройки — в SharedPreferences.
- Выгрузка записей: **Настройки → «Экспортировать журнал»** → CSV в `…/exports/downloads_*.csv`.

## 5. Скриншоты

15 headless-снимков (Paparazzi) в `app/src/test/snapshots/images/` (phone + tablet, light + dark,
все состояния).

## 6. Ограничения окружения

- **Эмулятор на этом хосте:** компонент `HypervisorPlatform` (WHPX) был отключён — **включён**,
  но нужна **перезагрузка** Windows, чтобы эмулятор получил аппаратное ускорение.
  WSL2 без nested virtualization (`/dev/kvm` нет) → KVM/QEMU в WSL недоступен.
  Поэтому установка/запуск на реальном Android здесь не выполнялись; сценарии проверены
  прогонами на JVM (Robolectric): реальная загрузка, персистентность, приём ссылок, запуск активити.
- Подпись — debug (release-ключ не предоставлен).
- YouTube-экстракция на устройстве не поддерживается (нужен yt-dlp + JS‑рантайм); скачивание — по прямым ссылкам.
