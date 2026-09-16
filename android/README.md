# VidFetch — Android-приложение

Нативное Android-приложение (Kotlin + Jetpack Compose + Material 3) для скачивания
файлов по прямым ссылкам. Живёт **в этом же (основном) репозитории** — отдельного
репозитория и git-сабмодулей нет.

## Возможности

- Вставка ссылки из буфера и проверка (HEAD; при отказе — GET+Range) с запуском реальной загрузки.
- Выгрузка журнала загрузок в CSV (Настройки → «Экспортировать журнал»).
- Локальная база **Room** (`vidfetch.db`): активные загрузки и история сохраняются между запусками.
- Пауза / продолжить / отмена / удаление; прогресс и размер берутся из БД.
- Настройки (тема, папка, число одновременных загрузок, язык) сохраняются (SharedPreferences).
- Тёмная / светлая / системная тема; нижняя навигация, 4 экрана.
- Состояния `loading` / `empty` / `error` / `content`.

> Экстракция YouTube на самом устройстве требует yt-dlp (Python) — вне объёма.
> Приложение скачивает по прямым `http(s)://`-ссылкам.

## Требования к окружению

- JDK 17
- Android SDK: platform `android-34`, build-tools `34.0.0`
- Gradle 8.5 (или `./gradlew`)

`local.properties` (не коммитится): `sdk.dir=<путь к Android SDK>`

## Сборка

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease         # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest       # юнит-тесты (Room, реальная загрузка, запуск активити)
./gradlew recordPaparazziDebug    # пере-записать скриншоты (app/src/test/snapshots)
```

Одна стандартная команда из чистой копии: `./gradlew assembleDebug`.
Готовые APK версии 1.1.0: `dist/VidFetch-1.1.0-debug.apk`, `dist/VidFetch-1.1.0-release.apk`
(каталог `dist/` не коммитится).

## Версия и подпись

- Версия: `app/build.gradle.kts` → `versionCode` / `versionName` (сейчас `2` / `1.1.0`).
- Release-подпись: положить в `local.properties` (не коммитится) ключи
  `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.
  Без них release-сборка подписывается debug-ключом (устанавливается, но не для стора).

## Установка

```bash
adb install -r dist/VidFetch-1.1.0-debug.apk
```

## Структура

```
app/src/main/java/app/vidfetch/mobile/
├── MainActivity.kt          # shell: Scaffold + нижняя навигация + NavHost
├── VidFetchApp.kt           # Application: Room + репозиторий + настройки
├── data/                    # DownloadEntity/Dao/AppDatabase, Downloader (реальная загрузка), SettingsStore
└── ui/                      # Model, Components, Screens, ViewModels, theme/Theme
app/src/test/java/...         # Robolectric-тесты + Paparazzi-скриншоты
app/src/test/snapshots/...    # сгенерированные скриншоты экранов
docs/                         # SCREENS.md, VERIFICATION.md
```

## Проверка

Отчёт: [`docs/VERIFICATION.md`](docs/VERIFICATION.md) — сборка, тесты, скриншоты,
ограничения (эмулятор на хосте недоступен).
