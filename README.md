# True IDLE — Android Edition

A persistent idle RPG built natively for Android using Kotlin and Jetpack Compose.

## Overview
True IDLE features 10 interconnected skills and systems where progression in one domain fuels power in all others:
- **Combat**: 100 Dungeon floors with auto-advance, boss milestones, active health/shield/mana pools, and category-filtered combat logs.
- **Player Stats & Dojo Training**: Upgrade STR, INT, VIT, LUCK, and Attack Speed with Gold.
- **Gathering (Farming & Mining)**: Cultivate alchemy herbs (Felweed, Bloodroot, Moonflower) and extract smithing ores (Copper, Tin, Iron).
- **Crafting (Smithing & Herblore)**: Forge high-tier weapons and heavy armor; brew restorative draughts and mana potions.
- **Beast Taming**: Defeat enemies for a chance to capture companion beasts, level up their skills, and merge star ranks.
- **Necromancy**: Harvest bone essence to raise Skeletons, Bone Mages, Dread Knights, and Bone Liches.
- **Inventory & Gear**: Equip weapons and armor, consume potions, and inspect item descriptions.
- **Achievements & Settings**: Offline progress calculation and persistent state.

## Tech Stack
- **Language**: Kotlin (see `gradle/libs.versions.toml` for the pinned version)
- **UI Framework**: Jetpack Compose with Material Design 3
- **Build System**: Gradle 8.14.6 with Kotlin DSL, driven by the committed wrapper
- **Android Target**: SDK 36, minSdk 26, JDK 21
- **Architecture**: MVVM with `StateFlow` and a coroutine-driven game loop (4 ticks/sec, 0.25 s per tick)

## Building the App
Always build through the wrapper so everyone (and CI) uses the same Gradle version:
```bash
./gradlew :app:assembleDebug
```
Run the unit tests and lint before pushing; CI gates on both:
```bash
./gradlew testDebugUnitTest lintDebug
```
The resulting debug APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`

A release build is signed only when a `keystore.properties` file exists at the repo root
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`); without it the release output is
left unsigned. The absolute keystore path should be kept out of version control.
