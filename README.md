# WoW Note

WoW Note is an offline-first Android notes app inspired by the fast, simple workflow of classic color-note apps, with a deliberately iOS-like visual system and richer typography/export tools.

## Core goals

- Unlimited text notes and checklists
- Color-coded notes, search, pin, archive and trash
- iOS-inspired cards, sheets, segmented controls and spacing
- Rich text editing: **bold**, *italic*, underline, text-size changes and paragraph alignment (left / center / right)
- Import custom `.ttf` / `.otf` fonts and reuse them in notes
- Export notes to `.docx` while preserving supported formatting
- Offline Myanmar calendar with Myanmar year/month/day, moon phase, Sabbath/Aphait and Gregorian date
- Local-first persistence; no account required

## Calendar attribution

The Myanmar calendar module follows the well-known Modern Myanmar Calendrical Calculations approach by Yan Naing Aye. The implementation in this project is kept local/offline and includes source-level attribution.

## Build

This repository uses a normal Android Gradle project. CI installs Gradle directly, so a binary Gradle wrapper JAR is not required in the repository.

```bash
gradle :app:assembleDebug
```

The GitHub Actions workflow uploads the generated debug APK as an artifact.
