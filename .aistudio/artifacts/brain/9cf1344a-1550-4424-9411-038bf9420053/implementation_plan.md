# Harvest Proficiency, High-Speed Combat Batching & Sci-Fi Iconography

Comprehensive performance, simulation, and UI expansion introducing automated harvest queues, exponential 2^level item proficiency scaling up to Level 100, floating-point harvest countdowns, lag-free high-speed combat kill batching, and replacement of emoji icons with styled Material sci-fi vector glyphs.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following requirements were confirmed during interactive clarification and govern the implementation:
> - **Iconography**: Replace all Unicode emojis (🪙, 💎, 💀, ⚔️, 🛡️, etc.) with Material vector icons tinted in sci-fi neon colors (`PrimaryNeon`, `SecondaryGold`, `NecroPurple`, `CyanShield`).
> - **Combat Batching**: When combat hit speed exceeds 10 hits/sec or when player DPS defeats multiple hostiles within a single simulation tick, transition seamlessly into high-speed batch calculation to prevent frame drops, lag, and lost kill efficiency.
> - **Harvest Proficiency**: Each herb and ore has an individual proficiency bar (Level 1 to 100). Yield scales exponentially at $2^{\text{level}-1}$ (doubling every level: 1x, 2x, 4x, 8x... up to $\approx 6.34 \times 10^{29}\times$ at Level 100) using 64-bit IEEE floating-point arithmetic and sci-fi compact notation (`e30`).
> - **Auto-Queue**: Players can enable 1 auto-queue slot in Bio-Farming and 1 auto-queue slot in Plasma Mining. Toggling an item auto-drills/auto-grows and collects continuously upon cycle completion.
> - **Harvest Timers**: Display remaining cycle durations with single-decimal float precision (e.g. `1.5s`, `0.1s`).

---

## 1. Overview & Core Concept

### What It Delivers
1. **Material Sci-Fi Iconography**: Replaces inconsistent system emojis with crisp, cohesive vector icons across currencies (Credits, Gems, Bone Essence), stats (ATK, DEF), and inventory loot.
2. **Lag-Free High-Speed Combat**: When attack speed or monster clearing outpaces UI rendering (>10 hits/sec or overkill damage across ticks), combat calculates batch kills, aggregate XP, credits, drops, and Slayer progression in a single analytical pass without frame hitching.
3. **Automated Harvest Queues**: A dedicated toggle per category (1 herb plot, 1 mining vein) allowing players to dedicate bio-drones and plasma drills to continuously harvest one specific resource hands-free.
4. **Item Proficiency (Levels 1–100) & Doubling Yield**: Each herb and ore accumulates proficiency XP per harvest. Each level doubles yield ($2^{\text{level}-1}$ multiplier). At Level 100, yields reach $2^{99}$ without numeric overflow.
5. **Precision Decimal Timers**: All active plot and vein cards display exact floating-point countdowns (`3.2s`, `0.4s`) rather than coarse percentages or integer rounding.

---

## 2. User Experience & Visual Design

### Key User Flows
- **Harvesting with Proficiency & Auto-Queue**:
  1. The player navigates to the **Skills -> Harvest** screen.
  2. Each **Plot** and **Vein** card shows:
     - Header: Item name, current Proficiency Level (`LVL 42/100`), and active multiplier badge (`x2.20e12 YIELD`).
     - Proficiency Progress Bar: Displays current item proficiency XP toward next level.
     - Cycle Timer: Floating-point countdown (`1.8s`, `0.3s`) alongside linear cycle progress.
     - Auto-Queue Toggle Button: Clean neon toggle (`AUTO ON / OFF`). Enabling auto-queue on "Felweed" disengages other herb plots, locking the dedicated bio-drone to Felweed.
     - Manual Harvest Button: Enabled when ready if not on auto-queue.
- **High-Speed Combat Feedback**:
  - When fighting high-level or lower-floor mobs with high DPS/attack speed, the HUD displays smooth continuous credit/XP flyouts and compact telemetry without spamming the combat log or freezing the main thread.
  - Periodic batch summary logs (`HIGH-SPEED ENGAGEMENT: x45 Hostiles Cleared (+12.4K CREDITS, +8.2K XP)`) keep the player informed without creating hundreds of GC allocations.
- **Currency & Stat Badges**:
  - Top app bar, dialogue windows, inventory chips, and shop buttons render Material vector icons (`MonetizationOn`, `Diamond`, `Dangerous`/Skull, `Shield`, `Bolt`) rendered at 16–20dp with matching neon tints.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Numeric Representation for $2^{100}$ Yields
- **Chosen Approach**: Store inventory item counts and harvest yields as 64-bit IEEE 754 `Double` (which natively supports values up to $1.79 \times 10^{308}$).
- **Why**: Standard integers (`Int` max $2.14 \times 10^9$) and 64-bit longs (`Long` max $9.22 \times 10^{18}$) overflow around levels 31 and 63 respectively. `Double` provides instantaneous arithmetic, requires zero third-party BigInteger dependencies, and integrates seamlessly with `NumberFormatter.short(Double)`.
- **Trade-Off**: Display uses scientific notation (`1.27e30`) once values exceed $10^{15}$, perfectly fitting sci-fi RPG aesthetics while preventing UI overflow.

### Decision 2: Single Dedicated Auto-Queue per Category
- **Chosen Approach**: Mutual exclusivity within each category (`HarvestState.plots` and `HarvestState.veins`). Selecting auto-queue on one item automatically sets `autoQueue = false` on all other items in that category.
- **Why**: Fulfills the user requirement for exactly 1 automation queue per category while preserving player agency to choose which resource they need most.

### Decision 3: Combat Overkill & High-Speed Batching
- **Chosen Approach**: When `totalDps * dt >= monster.maxHp * 2` or `hitsPerSec > 10.0`, compute `batchKills = floor(dealtDamage / monsterEhp)`. Scale rewards, XP, Slayer progress, and floor clearance proportionally in a single pass.
- **Why**: Prevents frame drops caused by repeatedly constructing 20–50 `Monster` and `LogEntry` objects per tick.

---

## 4. Technical Architecture & Data Strategy

### System & Component Architecture
```
┌────────────────────────────────────────────────────────┐
│                      TrueIdleApp                       │
│  - Top Bar: Replaces 🪙, 💎, 💀 with Neon Vector Icons  │
└───────────────────────────┬────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌────────────────────────┐      ┌────────────────────────┐
│     CombatScreen       │      │     HarvestScreen      │
│  - CombatTelemetryCard │      │  - Float Timers (1.5s) │
│  - Bulk Kill Animation │      │  - Proficiency Bar/Lvl │
│  - Scaled Rewards Pass │      │  - 1-Slot Auto Queue   │
└───────────┬────────────┘      └───────────┬────────────┘
            │                               │
            └───────────────┬───────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                      GameEngine                        │
│  - tickCombat: Batching kills when hitRate/DPS > 10/s  │
│  - tickHarvest: Auto-cycles 1 active plot & 1 vein    │
│  - Proficiency: Level up (1..100) -> 2^(lvl-1) yield   │
│  - Inventory: Double counts with NumberFormatter.short │
└────────────────────────────────────────────────────────┘
```

### Data Model Updates (`GameState.kt`)
```kotlin
// Updated Plot model
data class Plot(
    val id: Int = 0,
    val herbName: String = "Felweed",
    val growthProgress: Float = 0f,
    val durationSeconds: Float = 8f,
    val isReady: Boolean = false,
    val autoQueue: Boolean = false,
    val proficiencyLevel: Int = 1,
    val proficiencyXp: Long = 0L
)

// Updated MiningVein model
data class MiningVein(
    val id: Int = 0,
    val oreName: String = "Copper Ore",
    val reqLvl: Int = 1,
    val miningProgress: Float = 0f,
    val durationSeconds: Float = 5f,
    val isReady: Boolean = false,
    val autoQueue: Boolean = false,
    val proficiencyLevel: Int = 1,
    val proficiencyXp: Long = 0L
)

// Updated InventoryItem count to Double for extreme harvest scaling
data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val category: String = "Resource",
    val count: Double = 1.0,
    val tier: Int = 1,
    val slot: String = "",
    val statDesc: String = "",
    val sellValue: Long = 10L,
    val isEquipped: Boolean = false,
    val atk: Double = 0.0,
    val def: Double = 0.0
)
```

### Component & State Mapping
- `togglePlotAutoQueue(plotId: Int)`: Sets `autoQueue = !current` on selected plot, setting all other plots to `false`.
- `toggleVeinAutoQueue(veinId: Int)`: Sets `autoQueue = !current` on selected vein, setting all other veins to `false`.
- `computeProficiencyYield(level: Int)`: Returns $2^{\text{level}-1}$ multiplier (Level 1: 1x, Level 2: 2x, Level 3: 4x, Level 100: $\approx 6.34 \times 10^{29}\text{x}$).
- `NumberFormatter.short(Double)`: Displays compact numbers up to $10^{308}$ with precision scientific formatting.
