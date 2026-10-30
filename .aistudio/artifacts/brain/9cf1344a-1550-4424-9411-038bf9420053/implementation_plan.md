# System Upgrades, Combat Potions, Herb Alchemy & Dev Panel Expansion

Implementation plan to deliver comprehensive updates across True IDLE: simplifying the Skills menu to a single top-left button, converting Slayer missions to random kill-count directives, upgrading the Stats screen with full telemetry, replacing legacy gear drops with the canonical 13 equipment groups, adding batch selling (1x/100x/1000x) and rank/quantity sorting in the Arsenal, adding stacked quick HP/MP potion buttons on the combat panel, enabling simultaneous auto-activation for Power Talents, implementing exact 20-herb percentage brews, and building a modular debug Dev Panel.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed through Phase 1 user interview questions:
> - **Dev Panel Toggle**: Floating bug icon button placed at the top/bottom corner of the screen, active only in debug builds (`BuildConfig.DEBUG`).
> - **Combat Potion Controls**: Vertically stacked Health and Mana potion buttons on the combat HUD displaying active potion count, instant one-tap deployment, and an interactive popover/sheet to select specific potions and configure auto-deploy thresholds.
> - **Power Talents Activation**: All unlocked power talents (`rank > 0`) automatically fire whenever their cooldown expires, allowing multiple abilities to run simultaneously.

---

## 1. Overview & Core Concept

This expansion addresses gameplay friction, unlocks deeper tactical depth, and empowers developer testing:
- **Skills Navigation**: Cleans up the top bar by replacing busy multi-readouts with a single `[ ☰ SKILLS ]` menu button at the top-left, renaming all "Discipline" references to "Skills".
- **Slayer Mission Directives**: Decouples contracts from restrictive sector monster types. Missions now task the player with eliminating randomized quantities of hostiles (e.g. 5x, 7x, 11x, 25x, 1111x), scaling rewards and difficulty with contract tier.
- **Stats Screen Overhaul**: Expands the dashboard with detailed effective health pool (EHP), weapon/armor/node contributions across all 13 gear slots, total true DPS breakdown, and combat mitigation telemetry.
- **Equipment Drop & Migration Fix**: Sanitizes starting loadouts, drop tables, and saves so old equipment names ("Iron Greatsword", "Steel Plate", "Rusty Blade", "Tattered Leather", "Ember Band", "Warden Sigil") are completely migrated to the canonical 13 equipment groups (`F- Rifle`, `F- Vest`, `F- Blade`, `F- Visor`, `F- Node I`, etc.).
- **Arsenal Batch Selling & Sorting**: Introduces quick batch selling multipliers (`1x`, `100x`, `1000x`) and sort toggles (by Tier/Rank, Quantity, Slot) for frictionless inventory management.
- **Combat Potion Deployment**: Adds stacked HP and MP quick-action buttons directly to the combat panel showing active counts, manual deployment, and configurable auto-deploy triggers (e.g. auto-drink when HP < 30% / 50% / OFF).
- **Automated Simultaneous Power Talents**: Overhauls the talent loop so all active abilities run on individual timers and automatically trigger when off cooldown without blocking one another.
- **Accurate 20-Herb Crafting Engine**: Replaces generic potion effects with the exact 20-herb percentage formula table (direct missing HP/MP heals, regen over time, temporary shields, damage reduction, and Worldtree Leaf's permanent passive boost).
- **Modular Dev Panel**: Implements `DevPanel.kt` featuring resource grants (+100, +10k, Max), game speed multipliers (1x, 5x, 10x, 50x), stage/achievement/skill unlock cheats, hard reset, and live state inspection.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Cyberpunk Sci-Fi Aesthetics**: Dark carbon surfaces (`#0A0E17`), laser-sharp rectangular geometries, neon cyan (`#00E5FF`), gold (`#FFD700`), red alert (`#EF5350`), and shield blue (`#42A5F5`) accents.
- **Clean Single-Action Header**: Skills tab features a prominent top-left `[ ☰ SKILLS ]` trigger button with clear drawer slide animation.
- **Combat Potion Stack**: High-contrast, tactile buttons anchored alongside combat controls:
  - **HP Flask**: Crimson neon border, heart/vitality icon, remaining quantity badge, tap to consume, quick settings cog for auto-deploy threshold.
  - **MP Tonic**: Cyan neon border, energy icon, remaining quantity badge, tap to consume, quick settings cog for auto-deploy threshold.
- **Arsenal Controls**: Modern segmented pill selectors for `[1X] [100X] [1000X]` sell modes and sorting toggles (`[RANK ▲▼]`, `[QTY ▲▼]`).
- **Dev Panel Overlay**: Semi-transparent obsidian modal (`rgba(5, 8, 15, 0.94)`) with collapsible sections, high-contrast monospace readouts, and responsive touch targets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Slayer Target Mechanics**
  - *Chosen Approach*: Any monster defeat increments the active directive's count. Required kills are rolled from randomized thematic numbers (`5`, `7`, `11`, `25`, `50`, `111`, `333`, `777`, `1111`) based on tier rank.
  - *Why*: Eliminates awkward back-tracking to previous floors just to find a specific monster species, creating seamless idle progression while maintaining high reward scaling.
- **Decision 2: 20-Herb Percentage Engine**
  - *Chosen Approach*: Implement a rich `ActiveBuff` model supporting instantaneous heals (`missingHp * percentage`), time-based regeneration (`maxHpPerSec * duration`), damage reduction buffers, and passive inventory-based buffs.
  - *Why*: Perfectly satisfies the user's detailed specification for all 20 herbs without losing nuances like duration or mixed HP/MP recovery.
- **Decision 3: Modular Dev Panel Isolation**
  - *Chosen Approach*: Encapsulate all debug UI inside `DevPanel.kt` and guard button rendering with `BuildConfig.DEBUG`.
  - *Why*: Ensures zero debug pollution or performance overhead in release builds while giving full testing power during development.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          TRUE IDLE UI LAYER                            │
├───────────────┬───────────────────┬──────────────────┬─────────────────┤
│ CombatScreen  │   SkillsScreen    │ InventoryScreen  │   DevPanel      │
│ - Stacked HP/ │ - Single Top-Left │ - 1x/100x/1000x  │ - Currencies    │
│   MP Potions  │   Menu Button     │   Batch Selling  │ - Game Speed    │
│ - Auto-deploy │ - "Skills" naming │ - Rank/Qty Sort  │ - Unlocks/Reset │
│ - Quick-select│ - Sub-sections    │ - Gear Compare   │ - Live Inspector│
└───────┬───────┴─────────┬─────────┴────────┬─────────┴────────┬────────┘
        │                 │                  │                  │
        ▼                 ▼                  ▼                  ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        GAME VIEW MODEL & FLOW                          │
│ - usePotion() / autoPotion()              - batchSellItem()            │
│ - setAutoPotionThreshold()                - rerollSlayerTask()         │
│ - toggleDevPanel() / devModifyResource()  - devSetSpeed()              │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          GAME ENGINE CORE                              │
├─────────────────┬──────────────────┬─────────────────┬─────────────────┤
│ Potion Engine   │   Talent Loop    │  Slayer System  │ Equipment Drop  │
│ - 20 Herb buffs │ - Auto-trigger   │ - Any monster   │ - 13 canonical  │
│ - Missing HP/MP │ - Multi-active   │   random count  │   gear groups   │
│ - Worldtree Dew │ - Cooldown tick  │ - Auto-rewards  │ - Old item san. │
└─────────────────┴──────────────────┴─────────────────┴─────────────────┘
```

### Component & File Work Breakdown
1. **`app/build.gradle.kts`**: Enable `buildConfig = true` under `buildFeatures`.
2. **`app/src/main/java/com/aistudio/trueidle/model/GameState.kt`**:
   - Update default starting equipment in `inventory` to use canonical groups (`F- Rifle`, `F- Vest`, `F- Blade`, `F- Visor`, `F- Node I`, `F- Node II`).
   - Add potion auto-deploy configuration (`hpAutoThresholdPct`, `mpAutoThresholdPct`, `selectedHpPotion`, `selectedMpPotion`).
   - Add active status effects / temporary buffs list (`List<ActiveBuff>`).
3. **`app/src/main/java/com/aistudio/trueidle/core/ItemCatalog.kt`**:
   - Update potion definitions with the exact 20-herb percentage and duration formulas.
4. **`app/src/main/java/com/aistudio/trueidle/engine/GameEngine.kt`**:
   - Update `sanitize()` to migrate all legacy equipment items in saves to the 13 canonical groups.
   - Update `rollLoot()` to drop items from the 13 canonical groups scaled to floor/tier.
   - Implement potion consumption with exact percentage math and buff durations.
   - Implement automated simultaneous power talent execution on tick.
   - Update Slayer contract generator to target randomized kill counts (`5x`, `7x`, `11x`, `25x`, `1111x`) for any hostile.
   - Add cheat methods for Dev Panel: `devAddCurrency`, `devUnlockAll`, `devHardReset`.
5. **`app/src/main/java/com/aistudio/trueidle/ui/screens/SkillsScreen.kt`**:
   - Keep only 1 menu button at top-left (`[ ☰ SKILLS ]`), rename all labels to "Skills".
6. **`app/src/main/java/com/aistudio/trueidle/ui/screens/SlayerScreen.kt`**:
   - Display randomized hostile target count with progress bars and instant rewards.
7. **`app/src/main/java/com/aistudio/trueidle/ui/screens/StatsScreen.kt`**:
   - Enhance with detailed telemetry: EHP, True DPS breakdown, defense mitigation curve, and gear loadout metrics.
8. **`app/src/main/java/com/aistudio/trueidle/ui/screens/InventoryScreen.kt`**:
   - Add `1x`, `100x`, `1000x` batch selling options.
   - Add `Sort by Rank` and `Sort by Quantity` buttons.
9. **`app/src/main/java/com/aistudio/trueidle/ui/screens/CombatScreen.kt`**:
   - Add vertically stacked quick-deploy buttons for HP and MP potions with count badges and auto-deploy settings.
10. **`app/src/main/java/com/aistudio/trueidle/ui/screens/TalentScreen.kt`**:
    - Update Power Talents UI to show automated simultaneous execution status.
11. **`app/src/main/java/com/aistudio/trueidle/ui/components/DevPanel.kt`**:
    - Build modular Dev Panel with floating toggle button for debug APKs.
12. **Verification & Testing**:
    - Run `compile_applet` and execute unit tests (`gradle :app:testDebugUnitTest`) verifying all updated systems.
