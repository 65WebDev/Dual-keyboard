# DualKey — Two languages, one keyboard

[Download APK](https://github.com/65WebDev/Dual-keyboard/releases) · [Русская версия](#русская-версия)

DualKey is an experimental Android keyboard that displays two layouts simultaneously. English is always available, and a second language can be selected in settings. The active layout uses larger characters while the other remains visible.

## Features

- Dual-language key labels, globe switching and configurable letter sizes.
- Offline gesture typing with a visible trail, live word candidates and three suggestions.
- Smart spacing before the next word, automatic capitalization and two-second whole-word undo using Backspace.
- Swipe from Shift, ?123 or the globe to enter one uppercase letter, symbol or alternate-language character; straight trail, key highlight and popup preview.
- Spacebar swipe to move the cursor; long press to select the system keyboard.
- Accelerating Backspace repeat, adjustable vibration (defaults: 75%, 30 ms), light and dark themes, adjustable key height.
- English and Russian dictionaries bundled; additional dictionaries downloadable on demand and stored locally.

## Languages

English + Russian, Ukrainian, German, French, Spanish, Italian, Portuguese, Polish, Turkish or Greek. Additional layouts and gesture recognition are experimental. Long press offers selected accented characters. Profanity filtering currently covers bundled English and Russian dictionaries only.

## Installation

1. Download the APK from Releases and install it.
2. Open DualKey, enable it in Android input settings and select it.
3. Choose the second language and download its dictionary if needed.

Android 8.0 (API 26) or later. Internet access is used for dictionary downloads; gesture decoding runs locally. The application is under active development; compatibility and recognition quality may vary.

## Build and dictionary sources

JDK 17, Gradle 8.9, Android SDK 35. Run `python3 -m pip install wordfreq==3.1.1`, `python3 scripts/expand_dictionaries.py`, then `gradle assembleDebug`. GitHub Actions builds and publishes APKs.

Bundled frequency lists: [wordfreq](https://github.com/rspeer/wordfreq). Additional lists: [FrequencyWords](https://github.com/hermitdave/FrequencyWords), based on OpenSubtitles. Dictionary data is subject to upstream licensing and attribution requirements.

---

## Русская версия

DualKey — экспериментальная клавиатура Android, одновременно отображающая две раскладки. Английский доступен всегда, второй язык выбирается в настройках. Активные буквы крупнее, а альтернативные остаются видимыми.

### Возможности

- Две раскладки на клавишах, переключение глобусом и настройка размера букв.
- Офлайн-ввод свайпом с цветным следом, динамическим распознаванием и тремя подсказками.
- Умный пробел перед следующим словом, автоматическая заглавная буква, удаление распознанного слова целиком в течение двух секунд.
- Свайп от Shift, ?123 или глобуса для однократного ввода заглавной буквы, спецсимвола или символа другой раскладки. Прямая линия, подсветка и всплывающий символ.
- Перемещение курсора свайпом по пробелу, длительное нажатие пробела для выбора системной клавиатуры.
- Ускоренное удаление при удержании Backspace, настройка вибрации (75%, 30 мс по умолчанию), светлая и тёмная темы, настройка высоты клавиш.
- Встроенные русский и английский словари, скачивание остальных по необходимости и хранение на устройстве.

### Языки

Английский в сочетании с русским, украинским, немецким, французским, испанским, итальянским, португальским, польским, турецким или греческим. Дополнительные раскладки и распознавание экспериментальные. Некоторые дополнительные буквы доступны длительным нажатием. Фильтрация ненормативной лексики пока действует только для встроенных словарей.

### Установка

1. Скачайте APK из [Releases](https://github.com/65WebDev/Dual-keyboard/releases) и установите.
2. Откройте DualKey, включите клавиатуру в системных настройках и выберите её.
3. Выберите второй язык и при необходимости скачайте словарь.

Android 8.0 и выше. Интернет используется для загрузки словарей, распознавание выполняется локально. Приложение находится в активной разработке.

### Сборка и словари

JDK 17, Gradle 8.9, Android SDK 35. Установите wordfreq 3.1.1, выполните `python3 scripts/expand_dictionaries.py` и `gradle assembleDebug`. APK публикуются через GitHub Actions.

Источники словарей: [wordfreq](https://github.com/rspeer/wordfreq) и [FrequencyWords](https://github.com/hermitdave/FrequencyWords) (OpenSubtitles). На данные распространяются условия лицензий исходных проектов.
