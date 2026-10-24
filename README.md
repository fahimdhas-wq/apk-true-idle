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
- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose with Material Design 3
- **Build System**: Gradle 9.3.1 with Kotlin DSL (`build.gradle.kts`)
- **Android Target**: SDK 36, minSdk 26
- **Architecture**: MVVM with `StateFlow` and Coroutine-driven game loop (4 ticks/sec)

## Building the App
Run the standard Gradle command:
```bash
gradle :app:assembleDebug
```
The resulting debug APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`
