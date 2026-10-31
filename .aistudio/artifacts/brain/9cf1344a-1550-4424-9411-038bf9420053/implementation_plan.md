# Polish, Terminology Naturalization, Combat Loadout & Daily Rewards

Detailed implementation plan for addressing user feedback across True IDLE: standardizing Training button dimensions, naturalizing game terminology away from excessive sci-fi jargon, overhauling the Talent tab UI, removing Bone Essence from the top header, repositioning the Sector badge into the floor control bar, displaying the complete 13-slot equipment loadout on the Combat tab, reducing Harvest XP requirements by 60%, and implementing a dual-track (7-Day Sprint & 30-Day Calendar) Daily Login Rewards system inside the Stats tab.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed through Phase 1 interview questions:
> - **Daily Login Placement**: Dedicated calendar card inside the Stats tab with clear claim status and streak trackers.
> - **Login Reward Structure**: Dual tabs allowing the player to toggle between the high-reward 7-day sprint and the monthly 30-day calendar track.
> - **Combat Equipment Display**: Two clean, stacked cards for Weapons (3 slots: Rifle, Sidearm, Blade) and Armor with Nodes (10 slots: Visor, Vest, Grips, Pants, Boots, Duster, Rig, Node I-III).

---

## 1. Overview & Core Concept

This update refines True IDLE's interface ergonomics, progression pacing, and terminology:
- **Button Uniformity in Training**: Establishes fixed, consistent dimensions for all node rank badges and enhance action buttons, eliminating uneven row heights.
- **Natural Terminology**: Tones down dense sci-fi jargon ("Protocol", "Matrix", "Struggles", "Directive", "Telemetry") in favor of clear, accessible RPG titles ("Hero Stats", "Combat Passives", "Active Skills", "Contracts", "Battle Status").
- **Talent Tab UI Overhaul**: Redesigns the Talent screen with cohesive dark cyberpunk card styling, clear passives/skills grouping, and readable bonus previews.
- **Top Header Streamlining**: Removes the Bone Essence indicator from the top header bar to prevent clutter, and relocates the `SECTOR XX` banner to sit neatly in the center of the Combat floor navigation card.
- **Full Combat Equipment Loadout**: Upgrades the Hero Protocol combat panel to display all 13 equipped gear slots grouped into Weapons (Rifle, Sidearm, Blade) and Armor & Nodes (Visor, Vest, Grips, Pants, Boots, Duster, Rig, Node I-III).
- **Harvest XP Curve Rebalance**: Decreases mining and farming level-up XP requirements by 60% (`* 0.40`), making gathering progression 2.5x faster and far more rewarding.
- **Dual-Track Daily Login Rewards**: Introduces a dedicated daily login card in the Stats tab with a 7-day starter sprint (massive Gold & Diamonds) and a 30-day monthly calendar (Gold, Diamonds, Faith, Bone Essence, and Potions).

---

## 2. User Experience & Visual Design

### Terminology Naturalization Table
| Previous Hard Sci-Fi Text | Natural & Clear RPG Term |
| :--- | :--- |
| `HERO PROTOCOL` | `HERO STATS` |
| `THE STRUGGLE // KINETIC ADAPTATION` | `SURVIVAL PASSIVES` |
| `TACTICAL COMBAT OVERCLOCKS` | `ACTIVE SKILLS` |
| `TACTICAL TALENT PROTOCOL` | `TALENT TREE & SKILLS` |
| `TACTICAL DIRECTIVE` | `SLAYER CONTRACTS` |
| `CLASH TELEMETRY STATUS BANNER` | `BATTLE STATUS` |
| `COMBAT DPS MATRIX TELEMETRY` | `COMBAT BREAKDOWN` |
| `EXTRACTION MATRIX` | `HARVEST & EXTRACTION` |
| `FABRICATION MATRIX` | `EQUIPMENT FORGE` |

### Floor Navigation & Sector Header Redesign
```
┌───────────────────────────────────────────────────────────────┐
│ [ ◄ ] [ FLOOR 05 ] [ ► ]   [ SECTOR 05 ]   [ AUTO: ON ]       │
└───────────────────────────────────────────────────────────────┘
```
- The `SECTOR XX` pill sits prominently in the middle between floor steppers and the auto-advance toggle, freeing up top header space.

### Combat Equipment Loadout
```
┌───────────────────────────────────────────────────────────────┐
│ ⚔ WEAPONS LOADOUT (+48 ATK)                                   │
│  [F- RIFLE] +12 ATK   ·   [F- SIDEARM] +12 ATK  ·   [F- BLADE]│
├───────────────────────────────────────────────────────────────┤
│ 🛡 ARMOR & ACCESSORIES (+56 DEF, +28 VIT)                      │
│  [VISOR] [VEST] [GRIPS] [PANTS] [BOOTS] [DUSTER] [RIG]        │
│  [NODE I] [NODE II] [NODE III]                                │
└───────────────────────────────────────────────────────────────┘
```

### Daily Login Rewards UI (Stats Screen)
- **Header**: Current login streak and timer until the next daily reset.
- **Dual Tabs**: `[ 7-DAY SPRINT ]` and `[ 30-DAY CALENDAR ]`.
- **Card Matrix**: Daily tiles displaying day number, reward icons (Gold, Diamonds, Faith, Potions), amount badges, and status pills:
  - `CLAIMED`: Subdued border with green checkmark.
  - `CLAIM REWARD`: Glowing Neon Cyan button with ripple feedback.
  - `LOCKED`: Dark surface with lock icon.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Training Button Dimensions**
  - *Chosen Approach*: Enforce exact fixed widths and heights (`width = 76.dp, height = 40.dp` for Rank Box; `width = 130.dp, height = 40.dp` for Enhance Button) with single-line clipped numbers.
  - *Why*: Eliminates row resizing regardless of whether rank is 1 or 1,000,000.
- **Decision 2: Harvest XP Formula Modification**
  - *Chosen Approach*: Apply a `0.40` multiplier directly to `FARMING_XP_BASE` and `MINING_XP_BASE` in `XpCurves.kt`.
  - *Why*: Modifying the base constant in `core` guarantees that both live harvesting, offline progress, and UI progress bars remain 100% synchronized.
- **Decision 3: Daily Login Persistence**
  - *Chosen Approach*: Store `DailyLoginState` inside `GameState` with `lastClaimWallMs`, `sprint7ClaimedDays`, and `calendar30ClaimedDays`.
  - *Why*: Ensures login claims persist cleanly across restarts and app updates with zero risk of duplicate claims on the same calendar day.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────┐
│                       TRUE IDLE APP UI                        │
├───────────────┬───────────────────┬───────────────┬───────────┤
│ Top Bar       │   CombatScreen    │  StatsScreen  │ Training  │
│ - No Bone     │ - Full 13 Slots   │ - Daily Login │ - Uniform │
│ - Credits     │ - Center Sector   │   (7 & 30 D)  │   Buttons │
│ - Diamonds    │ - De-jargonized   │ - Clear terms │ - Fixed H │
└───────┬───────┴─────────┬─────────┴───────┬───────┴─────┬─────┘
        │                 │                 │             │
        ▼                 ▼                 ▼             ▼
┌───────────────────────────────────────────────────────────────┐
│                    GAME VIEW MODEL & ENGINE                   │
│ - claimDailyLoginReward(isSprint: Boolean)                    │
│ - trainNode() with unified dimensions                         │
│ - navFloor() & setAutoAdvance()                               │
└───────────────────────────────┬───────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────┐
│                         CORE & MATH                           │
│ - XpCurves: 60% XP reduction on farming & mining              │
│ - DailyRewardCatalog: 7-day sprint & 30-day calendar rewards  │
│ - Equipment: full 13-slot breakdown helpers                   │
└───────────────────────────────────────────────────────────────┘
```

### Component & File Work Breakdown
1. **`app/src/main/java/com/aistudio/trueidle/core/XpCurves.kt`**:
   - Reduce `FARMING_XP_BASE` and `MINING_XP_BASE` by 60% (`* 0.40`).
2. **`app/src/main/java/com/aistudio/trueidle/model/GameState.kt`**:
   - Add `DailyLoginState(lastClaimWallMs, sprint7ClaimedDays, calendar30ClaimedDays, totalLogins)`.
   - Add `dailyLogin: DailyLoginState = DailyLoginState()` to `GameState`.
3. **`app/src/main/java/com/aistudio/trueidle/core/DailyRewardCatalog.kt`**:
   - Define rewards for 7-day sprint (generous Gold & Diamonds up to 15M gold and 1000 diamonds).
   - Define rewards for 30-day calendar (Gold, Diamonds, Faith, Bone Essence, and Potions up to Worldtree Dew).
4. **`app/src/main/java/com/aistudio/trueidle/engine/GameEngine.kt`**:
   - Implement `claimDailyLogin(isSprint: Boolean): GameState`.
5. **`app/src/main/java/com/aistudio/trueidle/viewmodel/GameViewModel.kt`**:
   - Expose `claimDailyLogin(isSprint: Boolean)`.
6. **`app/src/main/java/com/aistudio/trueidle/ui/TrueIdleApp.kt`**:
   - Remove Bone Essence badge from the top header bar.
   - Remove Sector badge from the top bar (relocated to floor controls).
7. **`app/src/main/java/com/aistudio/trueidle/ui/components/FloorSelector.kt`**:
   - Place the `SECTOR XX` banner right in the middle between floor navigation and auto-advance.
8. **`app/src/main/java/com/aistudio/trueidle/ui/screens/CombatScreen.kt`**:
   - Display full 13 equipped gear slots grouped under Weapons (Rifle, Sidearm, Blade) and Armor with Nodes (Visor, Vest, Grips, Pants, Boots, Duster, Rig, Node I-III).
   - Clean up sci-fi jargon in titles.
9. **`app/src/main/java/com/aistudio/trueidle/ui/screens/TrainingScreen.kt`**:
   - Enforce fixed, uniform dimensions on rank boxes and enhance buttons (`height = 40.dp`).
10. **`app/src/main/java/com/aistudio/trueidle/ui/screens/TalentScreen.kt`**:
    - Complete UI makeover with modern cards, clear terms ("Survival Passives", "Active Skills"), and harmonious color accents.
11. **`app/src/main/java/com/aistudio/trueidle/ui/screens/StatsScreen.kt`**:
    - Add the Daily Login Rewards card with `[ 7-DAY SPRINT ]` and `[ 30-DAY CALENDAR ]` tabs.
    - Naturalize terminology across all stats sections.
12. **Verification & Testing**:
    - Run `compile_applet` and execute unit tests (`gradle :app:testDebugUnitTest`) verifying reward claims, 60% XP reduction, and UI layouts.
