# TRUE IDLE — Specification

Native Android sci-fi text idle RPG. Kotlin 2.1, Jetpack Compose (Material 3), `StateFlow`, kotlinx.serialization. XML is used only for manifest, themes, strings, colors, fonts, icons.

Repository: `fahimdhas-wq/apk-true-idle` · audited at commit `a2ab4ab` (55 files, 6,042 lines of Kotlin) · package `com.aistudio.trueidle` · `applicationId com.aistudio.trueidle.kmpqxr` · minSdk 26 · compile/targetSdk 36.

This document has two jobs: (1) record what the code actually does, including defects; (2) define the target behavior and the order to reach it. Every defect cited was verified in source.

---

## 1. Verified current state

### 1.1 What exists

| Area | Implementation |
|---|---|
| Shell | `MainActivity` → `TrueIdleApp`: top status bar, 5-tab bottom bar (`COMBAT · SKILLS · STATS · ARSENAL · SYSTEM`), `when(currentTab)` switch. No `NavHost` (navigation-compose is declared but unused). |
| State | One immutable `GameState` data class, replaced with `copy()` every tick. `GameEngine` holds it in `var state`. |
| Loop | `GameViewModel.startGameLoop()`: `while(true){ delay(250); engine.tick(0.25f) }` in `viewModelScope`. |
| Combat | 100 floors, boss every 10th, 10 kills per floor, auto-advance, hero HP/shield/mana, monster HP/shield/armor/damage. |
| Skills (10) | Combat (`player.lvl`), Farming, Mining, Smithing, Herblore, Beast Taming, Necromancy, Training (Dojo), Sanctified, Slayer. |
| Gathering | 3 crop plots + 3 ore veins, all progress in parallel, manual harvest. |
| Crafting | 4 smithing recipes (ore → gear), 3 brewing recipes (herb → potion). Manual. |
| Beasts | ≤ 8; 15% tame chance per kill; merge two with same name and stars → +1 star. |
| Necromancy | Bone essence from kills; summon Skeleton / Bone Mage / Dread Knight / Bone Lich. |
| Dojo | 5 talent nodes, gold cost ×1.4 per rank. |
| Inventory | Equipment, potions, resources, trophies; equip/unequip; potion use. |
| Notifications | Banner queue (latest 3, anti-stacking), history dialog (cap 50), on/off toggle. |
| Offline | `OfflineLootDialog` + `pendingOfflineLoot`. |
| Save | Manual export/import of pretty-printed JSON via clipboard. |
| Theme | `Color.kt` neon palette on `#000000`, `SharpShapes` (0 dp radius), Orbitron font (`res/font/orbitron.ttf`), 72 `testTag`s. |

### 1.2 Defects (blocking → minor)

IDs are referenced by the roadmap in §13.

| ID | Sev | Defect | Evidence |
|---|---|---|---|
| D1 | Blocker | **Nothing is persisted.** `GameViewModel` builds `GameEngine()` with default state; no code writes to disk. Process death resets all progress. | `GameViewModel.kt` L13; no file/DataStore use anywhere |
| D2 | Blocker | **Offline progress cannot trigger.** `checkOfflineProgress()` runs once in `init`, compares against `lastSaveTimestamp` of a freshly created default state (= now). `tick()` also rewrites `lastSaveTimestamp = now` every 250 ms. | `GameEngine.kt` L24, L398 |
| D3 | Blocker | **Fresh clone does not assemble.** `signingConfigs.debugConfig` points to `${rootDir}/debug.keystore`, which is git-ignored and absent. No Gradle wrapper (`gradlew`, `gradle/wrapper`) is committed. `proguard-rules.pro` is referenced but absent. README states Gradle 9.3.1 with AGP 8.8.0 — verify compatibility, pin via wrapper. | `app/build.gradle.kts`, `.gitignore`, `git ls-files` |
| D4 | High | **Loop is not lifecycle-aware and not monotonic.** It keeps ticking in the background while the process lives, uses a fixed `0.25f` per `delay(250)` (drift, no catch-up clamp), and nothing reconciles background time. | `GameViewModel.kt` L23–31 |
| D5 | High | **Kill-heal is overwritten.** Kill branch sets `hp = maxHp, shield = maxShield`, then step 5 does `p.copy(hp = pHp, shield = pShield)` using pre-kill values. The level-up message "Health & Mana replenished" is false. | `GameEngine.kt` L346–357 vs L371 |
| D6 | High | **Monster armor is never used.** `Monster.armor` (= floor × 1.5) is generated and displayed but excluded from damage math. | `GameEngine.kt` L236–252 |
| D7 | High | **DPS formula is copied in three places** (engine tick, `CombatTelemetryCard`, `StatsScreen`). Display and simulation can drift silently. | `GameEngine.kt` L233–236, `CombatTelemetryCard.kt` L31–32, `StatsScreen.kt` L35–36 |
| D8 | High | **Offline claim recomputes loot instead of applying it.** `claimOfflineLoot` re-derives ore/herb counts from `timeAwaySeconds`; `dropsSummary` strings (e.g. Wolf Fang) are never converted to items. Offline ore/herb numbers are invented (`seconds/40`, `seconds/30`), not derived from gameplay. | `GameEngine.kt` L52–60, L84–99 |
| D9 | High | **Stats are mutated, not derived.** Dojo ranks directly overwrite `maxHp`, `maxMana`, `str`, `luck`, `atkSpeed`. `vit` and `intVal` are displayed but unused; `luck` is displayed as "% LOOT RATE" but loot chance is a hard-coded 0.35 and tame chance 0.15. Level-ups grant no stats. | `GameEngine.kt` L626–636, L287, L317 |
| D10 | High | **Equipment values disagree.** Equip applies `tier × 12` / `tier × 8` regardless of `statDesc`; Iron Greatsword shows "+18 Damage" but equips as 24; Steel Plate shows "+12 Defense" but equips as 16. | `GameEngine.kt` L674, L690 |
| D11 | Med | **Defeat is not a state.** On `pHp <= 0` the hero is instantly refilled and the monster respawned; no recovery period, no retreat after repeated failure. | `GameEngine.kt` L384–392 |
| D12 | Med | **Dead systems.** `SanctifiedState`, `SlayerState`/`SlayerTask`, and `Achievement` progress are never updated by the engine. `diamonds` is never earned or spent. Achievements are display-only. | grep: only default values and UI reads |
| D13 | Med | **Notification anti-stacking merges unrelated events.** Match condition `it.title == title \|\| (type && message)` collapses "SKILL LEVEL UP" for Farming and Mining into one row and replaces the message. Counter IDs restart at 1000 after reload. History cap is 50. | `GameEngine.kt` L156–163 |
| D14 | Med | **IDs from `System.currentTimeMillis()`** collide when two items are created in the same millisecond. | `GameEngine.kt` (`item_…`, `ore_…`, `craft_…`) |
| D15 | Med | **Text is too small.** `fontSize` literals: 8 sp ×40, 9 sp ×39, 7 sp ×5, 10 sp ×11. Orbitron (wide) at 7–9 sp carries decision data (enemy/hero stats, support roster, nav labels at 8 sp). | grep of `fontSize =` |
| D16 | Med | **Combat layout is side-by-side.** Player and enemy cards are `weight(1f)` siblings in one `Row` (each ~half width), and Beasts/Undead are two 68 dp cards at 8–9 sp. Narrow widths truncate. | `CombatScreen.kt` L87, L195, L310, L356 |
| D17 | Med | **Whole-state recomposition.** Every screen receives the full `GameState`; a new instance every 250 ms recomposes all consumers. | `TrueIdleApp.kt` |
| D18 | Low | Tab selection uses `remember`, lost on rotation. | `TrueIdleApp.kt` L44 |
| D19 | Low | `INTERNET` permission declared; no code uses the network. `allowBackup="true"` with no backup rules. Release `isMinifyEnabled = false`. No tests exist. | manifest, gradle |
| D20 | Low | Boss names repeat at floors 90/100 (8 names for 10 bosses). Level-up grants at most one level per kill. Single `orbitron.ttf` is mapped to three weights (no real bold unless the file is variable — verify). | `Balance.kt`, `Type.kt` |

---

## 2. Product definition

Portrait-first, text-driven sci-fi idle RPG with a compact terminal HUD. No sprites, no canvas. Offline-first: no network dependency.

Pillars:

1. **Glanceable.** Every line answers: what is happening, how much remains, what changed, what can I do.
2. **Correct before pretty.** Persistence, offline, and combat math are fixed before visual work.
3. **One source of truth.** One `CombatMath`, one `DerivedStats`; UI never recomputes rules.
4. **Derive, don't mutate.** Stats are computed from base + ranks + gear. Saves store inputs, not results.
5. **Cheap.** No always-on animation. UI receives slices, not the whole state.
6. **Interlocking.** Every resource has one source and at least two sinks (§4.4).

Scope guard: floors are capped at 100, so `Long`/`Double` suffice. A big-number type is out of scope unless the floor cap is removed.

---

## 3. Architecture

### 3.1 Layout (extends the existing packages)

```text
app/src/main/java/com/aistudio/trueidle/
├── MainActivity.kt                 lifecycle observer → viewModel.onForeground()/onBackground()
├── core/                           NEW  pure Kotlin, no android.* imports
│   ├── CombatMath.kt               DPS, mitigation, incoming damage (single source)
│   ├── DerivedStats.kt             stats from base + dojo + level + gear
│   ├── NumberFormatter.kt          short(), duration(), percent()
│   ├── GameClock.kt                interface + Android impl lives in runtime/
│   └── Ids.kt                      monotonic id generator (persisted)
├── engine/
│   ├── Balance.kt                  (existing) monsters, XP curves
│   ├── GameEngine.kt               (existing) facade; split by domain in Phase 3
│   ├── OfflineEngine.kt            NEW  closed-form offline
│   ├── NotificationCenter.kt       NEW  coalescing, history
│   └── (later) SlayerEngine.kt SanctifiedEngine.kt AchievementEngine.kt
├── model/GameState.kt              (existing) + SaveMeta, LifetimeStats, schema migrations
├── data/                           NEW
│   ├── SaveStore.kt                interface + FileSaveStore (atomic, JVM-testable)
│   ├── SaveRepository.kt           envelope, CRC32, prev-slot recovery
│   └── Migrations.kt
├── runtime/AndroidClock.kt         NEW  wall + elapsedRealtime + boot count
├── viewmodel/GameViewModel.kt      (existing) loop, lifecycle, slices, save scheduling
└── ui/
    ├── TrueIdleApp.kt              shell
    ├── theme/ Color.kt Theme.kt Type.kt   (existing; Type.kt rescaled)
    ├── components/                 (existing) + SciFiGauge, SciFiPanel, SegmentedTabs, SupportModule, RosterRow
    └── screens/                    (existing) Combat, Skills, Stats, Inventory, Settings + sub-screens
```

Rules:
- `core/`, `engine/`, `data/` import no `android.*`. Enforced by a JVM test that scans those directories for `import android.` (§12).
- The engine takes a `GameClock` and a `kotlin.random.Random`; it never calls `System.currentTimeMillis()` or the global `Random`.
- Exactly one game loop, owned by `GameViewModel`.
- The engine is single-threaded; all mutations happen on the main dispatcher. If `tick` p95 exceeds 4 ms on the reference device, confine the engine to `Dispatchers.Default.limitedParallelism(1)`.
- `GameState` is immutable; the engine replaces it. UI never mutates nested objects.

### 3.2 Clock

```kotlin
// core/GameClock.kt
interface GameClock {
    fun wallMs(): Long        // System.currentTimeMillis()
    fun elapsedMs(): Long     // SystemClock.elapsedRealtime(): monotonic, includes deep sleep
    fun bootCount(): Int      // Settings.Global.BOOT_COUNT
}
data class ClockSnapshot(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)
fun GameClock.snapshot() = ClockSnapshot(wallMs(), elapsedMs(), bootCount())
```

```kotlin
// runtime/AndroidClock.kt
class AndroidClock(private val context: Context) : GameClock {
    override fun wallMs() = System.currentTimeMillis()
    override fun elapsedMs() = SystemClock.elapsedRealtime()
    override fun bootCount() =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}
```

### 3.3 Loop and lifecycle (fixes D4)

The loop runs only while the activity is started. Background time is reconciled by the offline engine, never by ticking.

```kotlin
// viewmodel/GameViewModel.kt (shape)
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val clock: GameClock = AndroidClock(app)
    private val repo = SaveRepository(FileSaveStore(File(app.filesDir, "saves")))
    private val engine = GameEngine(clock = clock, random = Random.Default)
    private val _state = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _state.asStateFlow()

    private val _recovery = MutableStateFlow<SaveRepository.Loaded.Failed?>(null)
    val recovery: StateFlow<SaveRepository.Loaded.Failed?> = _recovery.asStateFlow()

    private var loop: Job? = null
    private var autosave: Job? = null
    private var booted = false

    fun onForeground() {
        if (!booted) { boot(); booted = true }
        engine.resume(clock.snapshot())            // offline reconciliation, once, idempotent
        _state.value = engine.state
        if (loop?.isActive != true) loop = viewModelScope.launch { runLoop() }
        if (autosave?.isActive != true) autosave = viewModelScope.launch { while (isActive) { delay(30_000); saveNow() } }
    }

    fun onBackground() {
        loop?.cancel(); loop = null
        autosave?.cancel(); autosave = null
        saveNow()
    }

    private suspend fun CoroutineScope.runLoop() {
        var last = clock.elapsedMs()
        var acc = 0L
        while (isActive) {
            delay(50)
            val now = clock.elapsedMs()
            acc += (now - last).coerceIn(0L, STEP_MS * MAX_STEPS)   // clamp: never replay a long gap
            last = now
            var n = 0
            while (acc >= STEP_MS && n < MAX_STEPS) { engine.tick(STEP_MS.toInt()); acc -= STEP_MS; n++ }
            if (n == MAX_STEPS) acc = 0                              // drop backlog, never spiral
            if (n > 0) _state.value = engine.state
        }
    }

    private fun boot() {
        when (val r = repo.load()) {
            is SaveRepository.Loaded.Ok        -> engine.restore(r.state)
            is SaveRepository.Loaded.Recovered -> { engine.restore(r.state); engine.notifyRecovered(r.from) }
            is SaveRepository.Loaded.Failed    -> _recovery.value = r     // never auto-reset
            SaveRepository.Loaded.Empty        -> Unit
        }
    }

    private fun saveNow() = repo.save(engine.snapshotForSave(clock.snapshot()))

    private companion object { const val STEP_MS = 250L; const val MAX_STEPS = 8L }
}
```

`MainActivity`:

```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = viewModel.onForeground()
            override fun onStop(owner: LifecycleOwner) = viewModel.onBackground()
        })
        setContent { TrueIdleTheme { TrueIdleApp(viewModel) } }
    }
}
```

`onForeground()` is the only caller of `engine.resume()`. `engine.tick()` no longer writes `lastSaveTimestamp`; save metadata is written only by `snapshotForSave`.

---

## 4. Simulation rules

### 4.1 Derived stats (fixes D9)

Base values come from `PlayerStats()` defaults. Dojo ranks, level, and gear are inputs. Formulas reproduce today's starting values exactly (HP 100, shield 25, mana 50, loot 35%, tame 15%) so current balance is preserved.

```kotlin
// core/DerivedStats.kt
data class Derived(
    val str: Double, val intVal: Double, val vit: Double, val luck: Double, val atkSpeed: Double,
    val maxHp: Double, val maxShield: Double, val maxMana: Double,
    val weaponDmg: Double, val armorDef: Double,
    val lootChance: Double, val tameChance: Double, val smiteBonus: Double
)

object DerivedStats {
    fun of(s: GameState): Derived {
        val base = PlayerStats()
        fun rank(id: String) = (s.training.nodes.firstOrNull { it.id == id }?.rank ?: 0).toDouble()
        val lvl = s.player.lvl
        val str = base.str + 5.0 * rank("str_bonus")
        val luck = base.luck + 5.0 * rank("luck_bonus")
        val weapon = s.inventory.firstOrNull { it.isEquipped && it.slot == "Weapon" }
        val armor = s.inventory.firstOrNull { it.isEquipped && it.slot == "Armor" }
        return Derived(
            str = str, intVal = base.intVal, vit = base.vit, luck = luck,
            atkSpeed = base.atkSpeed * 1.05.pow(rank("speed_bonus")),
            maxHp = 40.0 + 5.0 * base.vit + 10.0 * lvl + 50.0 * rank("vit_bonus"),      // 100 at lvl 1
            maxShield = 20.0 + 5.0 * lvl,                                                 // 25 at lvl 1
            maxMana = 30.0 + 2.0 * base.intVal + 25.0 * rank("int_bonus"),                // 50 at lvl 1
            weaponDmg = weapon?.atk ?: 2.0,
            armorDef = armor?.def ?: 0.0,
            lootChance = 0.30 + luck / 100.0,                                             // 0.35 at luck 5
            tameChance = 0.10 + luck / 100.0,                                             // 0.15 at luck 5
            smiteBonus = if (s.sanctified.aegisActive) s.sanctified.smiteLevel * 3.0 else 0.0
        )
    }
}
```

`PlayerState` stops storing `maxHp`, `maxShield`, `maxMana`, `weaponDmg`, `armorDef` and `stats`; it keeps current `hp`, `shield`, `mana`. `InventoryItem` gains `atk: Double = 0.0` and `def: Double = 0.0` (numeric, authoritative); `statDesc` becomes a display string generated from them (fixes D10). Initial items: Rusty Blade atk 8, Iron Greatsword atk 18, Tattered Leather def 4, Steel Plate def 12. Crafted items use the recipe's stated values (+12, +24, +8, +16).

### 4.2 Combat math (fixes D6, D7)

```kotlin
// core/CombatMath.kt
object CombatMath {
    const val ARMOR_K = 100.0                       // mitigation = K / (K + armor)
    fun mitigation(armor: Double) = ARMOR_K / (ARMOR_K + armor.coerceAtLeast(0.0))

    fun heroDps(d: Derived) = (d.str * 1.2 + d.weaponDmg + d.smiteBonus) * d.atkSpeed
    fun beastDps(b: BeastTamingState) =
        b.beasts.sumOf { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) }
    fun armyDps(n: NecromancyState) =
        n.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) }
    fun totalDps(s: GameState, d: Derived) = heroDps(d) + beastDps(s.beasts) + armyDps(s.necro)

    /** Damage per second the hero takes after armor, before shield. Floored at 1/s as today. */
    fun incomingPerSec(m: Monster, d: Derived, aegis: Boolean): Double {
        val raw = m.damage * if (aegis) 0.8 else 1.0
        return (raw - d.armorDef * 0.3).coerceAtLeast(1.0)
    }
}
```

`CombatTelemetryCard`, `StatsScreen`, `GameEngine.tick`, and `OfflineEngine` all call `CombatMath`. No other file contains a DPS formula.

Balance impact: introducing monster armor mitigation reduces effective damage (floor 50 → armor 75 → −43%). It is a balance change, not a bug fix in isolation. Ship it behind the balance pass in Phase 1: retune monster HP scaling or set `ARMOR_K` so floor-30 clear time stays in the 45–90 s target.

### 4.3 Combat tick order

Per 250 ms step:

1. If `recoverLeft > 0`: reduce it, regenerate, return (§4.5).
2. If `respawnLeft > 0`: reduce it (0.5 s gap between kills), return. (This gap is part of live combat so offline math matches.)
3. Hero + support damage to monster: `dps × dt × mitigation(monster.armor)`; shield first, then HP.
4. Monster damage to hero: `incomingPerSec × dt`; shield absorbs first; remainder to HP.
5. Mana regen (`+2/s`, capped by derived `maxMana`); Sanctified aegis drain (§4.9).
6. Kill resolution (once): gold, XP (loop level-ups, not one per kill), bones (1–3), loot roll (`lootChance`, 50/50 ore or herb), tame roll (`tameChance`, roster < 8), slayer progress, lifetime stats, floor progress, spawn next monster, set `respawnLeft`.
7. Hero defeat: enter recovery (§4.5).
8. Gather progress for plots and veins.

Hero HP/shield after a kill are **not** restored (the current attempt in D5 is removed). Level-up restores HP, shield, mana to derived maxima — this is the only full heal, and the message stays true.

### 4.4 Economy graph

| Resource | Source | Sinks (≥ 2) |
|---|---|---|
| Gold | kills, slayer tasks, achievements, selling | Dojo ranks, Sanctified smite upgrades, recipes (future gold fee), potion purchases (future) |
| XP (player) | kills | level-ups (stats, HP/shield/mana) |
| Bone Essence | kills | Necromancy summons, Smithing bone-alloy tier (Phase 4) |
| Herbs | plots, loot | Herblore brews, beast bait (Phase 4) |
| Ores | veins, loot | Smithing recipes, support plating (Phase 4) |
| Faith | kills (+1) | smite upgrades, aegis upkeep |
| Slayer Points | tasks | task reroll, permanent slayer bonuses |
| Diamonds | boss first-clears, directives | offline cap and efficiency upgrades, extra gather auto-collect |
| Trophies | beast kills | beast merge cost reduction (Phase 4), achievements |

Rule: a resource added without ≥ 2 sinks fails review.

### 4.5 Recovery state (fixes D11)

`PlayerState` gains `recoverLeft: Double` and `failuresInRow: Int`.

- `hp <= 0` → `recoverLeft = 6.0 s` (reduced by Dojo later), `killsOnFloor = 0`, `failuresInRow++`, log `SYSTEMS REBOOTING`.
- While recovering the clash line shows `RECOVERING 4s`; no damage either way.
- On completion: hp/shield restored to derived maxima, same floor.
- `failuresInRow >= 3` with auto-advance on: descend one floor, log `RETREAT PROTOCOL ENGAGED`, reset counter. Any kill resets `failuresInRow`.
- Never print "dead/died" in UI or logs.

### 4.6 Gathering and crafting

- Plots and veins progress in parallel as today. Harvest/mine is manual and yields 2 herbs / 3 ores.
- Farming/Mining level scale durations: `duration × 0.97^(lvl-1)`, floored at 40% of base.
- **Auto-collect** (new, removes chore): unlocked at Farming/Mining level 10 and again at 25 (collects ready nodes, optional toggle). Offline gathering uses the same rule: each node yields at most the harvests that fit in elapsed time **if auto-collect is unlocked**, otherwise it sits `READY` (no invented drops; fixes D8).
- Smithing: recipes list `have/need`, duration 0 (instant) in Phase 1–3; reason text when unavailable (`REQUIRES 4× COPPER ORE (HAVE 2)`).
- Herblore: same pattern; brewed potions restore HP as stated on the recipe (`statDesc` generated from the number the engine uses; today's text says 50/80 HP and the engine heals a flat 75 — make the recipe the source).

### 4.7 Beasts and Necromancy (identical shape)

Both are "support units" in UI and DPS math but keep separate models.

| | Beasts | Undead |
|---|---|---|
| Source | tame chance per kill, roster cap 8 | summon with Bone Essence (10 / 25 / 50 / 100), cap 12 |
| Scaling | rank × (1 + 0.25 × (stars − 1)) | count × (1 + 0.2 × (lvl − 1)) |
| Merge | two with same name and stars → +1 star, ×1.6 DPS, ×1.5 HP (current) | merge flow deferred (Phase 4) |
| Upkeep | none today | none today |

Support units absorb nothing today (`Minion.soak` and `Beast.hp` are unused). Decision: use them in Phase 3 — routing 15% of incoming damage to support units, who become `WOUNDED` for 60 s at 0 HP (never deleted). Until then, hide HP/soak from UI so it does not imply a mechanic.

### 4.8 Dojo, equipment

- Dojo nodes stay as defined (5 nodes, cost ×1.4 per rank, `maxRank` 30/30/30/15/20). They contribute rank only; `DerivedStats` reads them (§4.1).
- Equipment: one weapon, one armor. Compare shows `CURRENT` vs `NEW` with `▲/▼` deltas. Bulk sell confirms totals. Unequip reverts to Bare Hands (2) / None (0).

### 4.9 Slayer, Sanctified, Achievements, Diamonds (fixes D12)

**Slayer.** `SlayerTask(targetName, currentKills, requiredKills, rewardGold, rewardPoints)`. A kill whose `monster.name == targetName` increments progress. On completion: gold + points, Slayer XP, next task chosen deterministically from monsters of floors `[currentFloor−3, currentFloor+1]` using the engine `Random`. Points buy task reroll (1 point) and, later, permanent small bonuses.

**Sanctified.** Faith +1 per kill (+5 per boss). Smite level raises flat DPS (`+3 × level`, as today); upgrade cost `25 × 1.5^level` faith. Aegis (`aegisActive`) reduces incoming damage 20% and drains 0.5 faith/s; at 0 faith it switches off with a notice. Both toggles live in the Sanctified sub-screen.

**Achievements.** Evaluated on state changes from `LifetimeStats(totalKills, oresMined, herbsHarvested, beastsTamed, forged, brewed, defeats)`. Unlocking pushes a notification and makes the reward claimable (claim button, not auto-grant). Seed list: First Blood, Dungeon Crawler (floor 10), Beast Whisperer (3 beasts tamed ever), Lord of Bones (10 minions), Master Miner (50 ores) — now actually progressing.

**Diamonds.** +5 on each boss's first clear; directive rewards; sinks listed in §4.4.

### 4.10 Notifications (fixes D13)

- Key = `(type, title, message)`. Identical events within 2 s increment `count`; different messages never merge.
- Ids come from the persisted monotonic generator (§5.2).
- Banner shows the latest 3; history cap 300; history persisted in the save.
- Muting classes (`LOOT`, `CRAFT`, `SUPPORT`, `SYSTEM`) hides banners only; history still records. `CRITICAL` (defeat/retreat, save failure) bypasses mute. `HIDE BANNERS` and `CLEAR HISTORY` stay separate controls.

---

## 5. Persistence

### 5.1 Save envelope and store

```kotlin
// data/SaveStore.kt
interface SaveStore {
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray)     // atomic
}

class FileSaveStore(private val dir: File) : SaveStore {
    override fun read(name: String): ByteArray? =
        File(dir, name).takeIf { it.isFile }?.readBytes()

    override fun write(name: String, bytes: ByteArray) {
        dir.mkdirs()
        val tmp = File(dir, "$name.tmp")
        FileOutputStream(tmp).use { it.write(bytes); it.fd.sync() }
        Files.move(tmp.toPath(), File(dir, name).toPath(),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
```

```kotlin
// data/SaveRepository.kt
@Serializable
data class SaveEnvelope(
    val format: String = "true-idle",
    val schema: Int,
    val savedWallMs: Long,
    val crc32: String,            // CRC32 of payload UTF-8 bytes
    val payload: String           // JSON of GameState (a string, so the checksum covers exact bytes)
)

class SaveRepository(private val store: SaveStore, private val json: Json = SaveJson) {
    sealed interface Loaded {
        data object Empty : Loaded
        data class Ok(val state: GameState) : Loaded
        data class Recovered(val state: GameState, val from: String) : Loaded
        data class Failed(val reason: String, val raw: ByteArray?) : Loaded
    }

    fun save(state: GameState) {
        val payload = json.encodeToString(GameState.serializer(), state)
        val env = SaveEnvelope(schema = SCHEMA, savedWallMs = state.meta.savedWallMs,
            crc32 = crc(payload), payload = payload)
        val bytes = json.encodeToString(SaveEnvelope.serializer(), env).toByteArray(Charsets.UTF_8)
        check(decode(bytes) != null) { "save failed self-verification" }      // verify before swapping
        store.read(SLOT)?.takeIf { decode(it) != null }?.let { store.write(PREV, it) }
        store.write(SLOT, bytes)
    }

    fun load(): Loaded {
        val main = store.read(SLOT)
        decode(main)?.let { return Loaded.Ok(it) }
        val prev = store.read(PREV)
        decode(prev)?.let { return Loaded.Recovered(it, PREV) }
        return if (main == null && prev == null) Loaded.Empty
        else Loaded.Failed("SAVE_UNREADABLE", main ?: prev)
    }

    /** Accepts an envelope, or a legacy raw GameState JSON (the old clipboard export). */
    fun decode(bytes: ByteArray?): GameState? = bytes?.let {
        runCatching {
            val text = it.toString(Charsets.UTF_8)
            val root = json.parseToJsonElement(text).jsonObject
            if (root["format"]?.jsonPrimitive?.content == "true-idle") {
                val env = json.decodeFromJsonElement(SaveEnvelope.serializer(), root)
                if (env.schema > SCHEMA || env.crc32 != crc(env.payload)) return null
                Migrations.apply(json.parseToJsonElement(env.payload).jsonObject, env.schema, json)
            } else Migrations.apply(root, 0, json)            // schema 0 = legacy export
        }.getOrNull()
    }

    private fun crc(s: String) =
        CRC32().apply { update(s.toByteArray(Charsets.UTF_8)) }.value.toString(16).padStart(8, '0')

    companion object { const val SLOT = "slot"; const val PREV = "slot.prev"; const val SCHEMA = 1 }
}

val SaveJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
```

Behavior:
- Autosave: every 30 s while foreground, on `onStop`, and (debounced ≥ 5 s) after irreversible actions (craft, train, merge, summon, equip, sell, claim).
- Newer-than-known schema → refuse, offer raw export. Both slots unreadable → recovery dialog (export raw bytes, or start a new game with explicit confirmation). Never auto-reset.
- A failed save posts a `CRITICAL` notification once; the game keeps running.
- Settings (banner toggle, notation, reduced motion) are small and stored separately (Preferences DataStore or a `settings.json` via the same store); they are not part of the progression snapshot.

### 5.2 Model additions

```kotlin
@Serializable
data class SaveMeta(
    val savedWallMs: Long = 0L,
    val savedElapsedMs: Long = 0L,
    val bootCount: Int = 0,
    val offlineAppliedUntilWallMs: Long = 0L,
    val nextId: Long = 1L,                  // monotonic id source (items, logs, notifications)
    val rngSeed: Long = 0L
)

@Serializable
data class LifetimeStats(
    val totalKills: Long = 0L, val oresMined: Long = 0L, val herbsHarvested: Long = 0L,
    val beastsTamed: Long = 0L, val forged: Long = 0L, val brewed: Long = 0L, val defeats: Long = 0L
)
```

`GameState` gains `meta: SaveMeta` and `lifetime: LifetimeStats`; `lastSaveTimestamp` is removed. `Migrations.apply` upgrades schema 0 (clipboard export) by: filling `meta`, assigning `InventoryItem.atk/def` from `statDesc` (`"+18 Damage"` → 18; fallback `tier×12` / `tier×8`), dropping stored `maxHp/maxShield/maxMana/weaponDmg/armorDef/stats`, and seeding `nextId` above every existing numeric id.

### 5.3 Export / import UI

Primary path: Storage Access Framework (`ActivityResultContracts.CreateDocument` / `OpenDocument`), file name `true-idle-yyyyMMdd-HHmm.tis`. Existing clipboard path stays as a secondary option. Import is validated in memory (envelope, CRC, migration, invariants: no negative resources, level ≥ 1, ids unique, floor ≤ 100), shows a preview line (`LEVEL 41 · FLOOR 63 · SAVED 2026-10-24 19:10`), requires confirmation, then swaps atomically. A failed import changes nothing.

---

## 6. Offline progression

### 6.1 Elapsed time (fixes D2)

```kotlin
fun trustedAwayMs(meta: SaveMeta, now: ClockSnapshot, capMs: Long): Long {
    val raw = when {
        meta.savedWallMs == 0L -> 0L                                           // first launch
        meta.bootCount == now.bootCount && now.elapsedMs >= meta.savedElapsedMs ->
            now.elapsedMs - meta.savedElapsedMs                               // monotonic: immune to clock edits
        now.wallMs >= meta.savedWallMs ->
            minOf(now.wallMs - meta.savedWallMs, 10 * 60_000L)                // rebooted: wall clock, capped at 10 min
        else -> return -1L                                                    // clock moved backwards
    }
    return minOf(raw, capMs)
}
```

- Cap: 8 h default (current code uses 24 h); 24 h reachable through a Diamond upgrade. Efficiency 0.65 default, 0.90 max (also Diamond upgrades).
- Valid only if `away ≥ 30 s` and `now.wallMs > meta.offlineAppliedUntilWallMs`.
- `-1` → no rewards, notification `CLOCK ANOMALY DETECTED — OFFLINE GAINS SUSPENDED`.
- After computing: write `offlineAppliedUntilWallMs = now.wallMs` and save the snapshot **before** showing the dialog, so repeated resume awards zero.

### 6.2 Closed-form combat

```kotlin
// engine/OfflineEngine.kt
object OfflineEngine {
    const val SPAWN_GAP_S = 0.5            // identical to live respawnLeft
    const val SAFETY = 1.25

    private class FloorStats(val perKillS: Double, val survivable: Boolean, val m: Monster)

    private fun stats(f: Int, dps: Double, d: Derived, s: GameState): FloorStats {
        val m = Balance.generateMonsterForFloor(f)
        val effDps = dps * CombatMath.mitigation(m.armor)
        val ttk = (m.maxHp + m.maxShield) / maxOf(effDps, 1e-6)
        val incoming = CombatMath.incomingPerSec(m, d, s.sanctified.aegisActive)
        val ehp = d.maxHp + d.maxShield
        return FloorStats(ttk + SPAWN_GAP_S, ehp / incoming > ttk * s.player.killsRequired * SAFETY, m)
    }

    fun compute(s: GameState, d: Derived, awayMs: Long, efficiency: Double, rnd: Random): OfflineLoot {
        var remaining = awayMs / 1000.0 * efficiency
        val dps = CombatMath.totalDps(s, d)
        var floor = s.player.currentFloor
        val ceiling = s.player.maxUnlockedFloor                       // offline never unlocks new floors
        repeat(10) { if (floor > 1 && !stats(floor, dps, d, s).survivable) floor-- }
        var killsOnFloor = if (floor == s.player.currentFloor) s.player.killsOnFloor else 0
        var kills = 0L; var gold = 0L; var xp = 0L; var gained = 0
        val perFloorKills = mutableListOf<Pair<Int, Int>>()
        var guard = 0

        fun award(f: Int, k: Int) {
            if (k <= 0) return
            val m = Balance.generateMonsterForFloor(f)
            kills += k; gold += k * m.goldReward; xp += k * m.xpReward
            perFloorKills += f to k
        }

        while (remaining > 0 && guard++ < 500) {
            val st = stats(floor, dps, d, s)
            val need = s.player.killsRequired - killsOnFloor
            val clearS = need * st.perKillS
            val advance = s.player.autoAdvance && floor < ceiling && stats(floor + 1, dps, d, s).survivable
            if (advance && remaining >= clearS) {
                award(floor, need); remaining -= clearS; floor++; killsOnFloor = 0; gained++
            } else {
                val k = (remaining / st.perKillS).toInt()
                award(floor, k); remaining = 0.0
                killsOnFloor = (killsOnFloor + k) % s.player.killsRequired
            }
        }

        val lootRolls = binomial(rnd, kills.toInt(), d.lootChance)
        val ore = binomial(rnd, lootRolls, 0.5)
        val herb = lootRolls - ore
        val tamed = minOf(binomial(rnd, kills.toInt(), d.tameChance), 8 - s.beasts.beasts.size)
        return OfflineLoot(
            timeAwaySeconds = awayMs / 1000, kills = kills, goldEarned = gold, xpEarned = xp,
            bonesEarned = kills * 2, playerLevelsGained = 0 /* computed at commit */,
            floorsGained = gained, finalFloor = floor, efficiency = efficiency,
            capApplied = false,
            items = listOfNotNull(
                LootItem("Copper Ore", ore).takeIf { ore > 0 },
                LootItem("Felweed", herb).takeIf { herb > 0 }),
            beastsTamed = tamed.coerceAtLeast(0)
        )
    }

    /** Exact for small n, normal approximation otherwise; uses the engine Random so repeats cannot reroll. */
    fun binomial(r: Random, n: Int, p: Double): Int = when {
        n <= 0 || p <= 0.0 -> 0
        p >= 1.0 -> n
        n <= 30 -> (0 until n).count { r.nextDouble() < p }
        else -> {
            val u = r.nextDouble().coerceAtLeast(1e-12); val v = r.nextDouble()
            val z = sqrt(-2.0 * ln(u)) * cos(2.0 * PI * v)
            (n * p + sqrt(n * p * (1 - p)) * z).roundToInt().coerceIn(0, n)
        }
    }
}
```

`OfflineLoot` gains `floorsGained`, `finalFloor`, `efficiency`, `capApplied`, `items: List<LootItem>`, `beastsTamed`. `claimOfflineLoot` applies exactly those fields (gold, XP via a level-up loop shared with live combat, bones, items, tamed beasts) and nothing else — no recomputation.

Other systems offline: gather plots/veins complete harvest cycles only if auto-collect is unlocked, else become `READY`; Sanctified aegis drains faith for `awayMs` (floored at 0, then off); Slayer progress counts kills of the target within `perFloorKills`; achievements evaluate on commit.

### 6.3 Summary dialog

`OfflineLootDialog` shows: time away (`CAP 8h APPLIED` when capped), efficiency line, floors gained / final floor, kills, gold, XP, levels gained, bones, items with counts, beasts tamed. One `CLAIM` button; state is already committed (§6.1), so claim only dismisses and gives a haptic. The dialog is not shown if load/validation failed.

### 6.4 Parity requirement

For 10 min of away time at efficiency 1.0, closed-form gold is within ±3% of 2,400 ticks of `GameEngine.tick`, over 20 seeded builds (varying floor, gear, beasts, army). Enforced by a JVM test (§12).

---

## 7. UI system

### 7.1 Principles

- Phone first: 360 × 800 dp baseline, usable at 320 dp, font scale up to 2.0. Landscape/tablet: same single column, `widthIn(max = 560.dp)` centered.
- One column. Enemy and hero panels each use the full width.
- Text floor: **12 sp** for anything that drives a decision; **11 sp** only for timestamps and secondary telemetry. No 7–10 sp text (D15).
- No scanline overlays, glow blur, particles, or permanent animation. Bars may animate ≤ 180 ms unless reduced motion is on.
- Never color-only: every state carries text or a glyph.
- Semantic color use; keep `SharpShapes` (0 dp radius) and 1 dp borders.

### 7.2 Tokens (keep `Color.kt`; add what is missing)

Existing: `BgDark #000000`, `SurfaceDark #07090E`, `SurfaceVariantDark #0F131C`, `BorderDark #182232`, `PrimaryNeon #00E5FF`, `SecondaryGold #FFE600`, `NecroPurple #C77DFF`, `HpRed #FF0055`, `ShieldBlue #00B4D8`, `ManaTeal #00F0FF`, `GreenSuccess #00FF66`, `TextMuted #7C889C`, `TextLight #FFFFFF`.

Add: `Warn = Color(0xFFFF9F43)` (recovery, caps), `SurfaceRaised = Color(0xFF162234)` (selected rows, bar tracks). `TextMuted` on `#000000` is ~6:1 — keep. `res/values/colors.xml` stays only for the window/status-bar theme.

### 7.3 Typography (fixes D15, D20)

Orbitron is wide; at small sizes it consumes ~20% more width than a sans face. Use it for titles, tags, and navigation only; use the system sans family for values and body so `420 / 420` fits at 320 dp.

```kotlin
// ui/theme/Type.kt (replace the fixed small sizes)
object TrueIdleType {
    val title = TextStyle(fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.8.sp)
    val tag = TextStyle(fontFamily = OrbitronFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp)
    val label = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp)
    val value = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum")
    val body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, lineHeight = 18.sp)
    val meta = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 14.sp)
}
```

Map the Material3 `Typography` to these so unmigrated screens inherit the floor: `labelSmall`, `bodySmall` ≥ 11 sp; `labelMedium` ≥ 12 sp. Verify `orbitron.ttf` is a variable font; if it is static, add a bold file or drop the weight claims.

All sizes are `sp`; no fixed-height containers around text (replace the `height(68.dp)` support cards with `heightIn(min = …)`). Numeric cells use `Modifier.widthIn(min = 72.dp)` so changing values do not reflow rows.

### 7.4 Shell

- Top bar: `TRUE IDLE · SECTOR 07` left; gold and bones right. Add `WindowInsets.displayCutout` horizontal padding. Tapping a resource opens its breakdown sheet.
- Bottom bar: five tabs `COMBAT · SKILLS · STATS · ARSENAL · SYSTEM`, each ≥ 56 dp tall, 11 sp label, `selectable(role = Role.Tab)`, active state = cyan text + 2 dp top indicator (no glow).
- `currentTab` uses `rememberSaveable` (D18). Back: close dialog/sheet → return to COMBAT → exit.
- Remove `navigation-compose` from dependencies unless a `NavHost` is adopted.

```kotlin
@Composable
fun BottomTab(tab: TrueIdleTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = if (selected) PrimaryNeon else TextMuted
    Column(
        modifier
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .drawBehind { if (selected) drawRect(PrimaryNeon, size = Size(size.width, 2.dp.toPx())) }
            .padding(vertical = 6.dp)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(tab.label, style = TrueIdleType.tag, color = tint, maxLines = 1)
    }
}
```

### 7.5 Components

`SciFiPanel(title, accent, tag?) { content }` — `SurfaceDark`, 1 dp border, 12 dp padding, 0 dp radius.

`SciFiGauge` — label left, value right, bar full width beneath (no side-by-side compression):

```kotlin
@Composable
fun SciFiGauge(
    label: String, valueText: String, ratio: Float, color: Color,
    modifier: Modifier = Modifier
) {
    val r = ratio.coerceIn(0f, 1f)
    Column(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label $valueText"
            progressBarRangeInfo = ProgressBarRangeInfo(r, 0f..1f)
        }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(label, style = TrueIdleType.label, color = TextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(valueText, style = TrueIdleType.value, color = TextLight,
                maxLines = 1, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 72.dp))
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier.fillMaxWidth().height(6.dp).background(SurfaceRaised)
                .drawBehind { drawRect(color, size = Size(size.width * r, size.height)) }
        )
    }
}
```

`SegmentedTabs(options, selectedId, onSelect)` — equal-weight `Row` of `selectable` cells (≥ 48 dp), selected = `SurfaceRaised` + 2 dp cyan underline; used for Beasts/Undead and Skills categories. `RosterRow(name, stars, dps, meta, wounded)` is shared by Beasts and Undead so their geometry is identical by construction:

```kotlin
@Composable
fun SupportModule(
    beasts: SupportGroupUi, undead: SupportGroupUi,
    onManage: (SupportKind) -> Unit, modifier: Modifier = Modifier
) {
    var kind by rememberSaveable { mutableStateOf(SupportKind.BEAST) }
    val group = if (kind == SupportKind.BEAST) beasts else undead
    SciFiPanel(title = "SUPPORT", accent = if (kind == SupportKind.BEAST) GreenSuccess else NecroPurple, modifier = modifier) {
        SegmentedTabs(
            options = listOf(
                SegOption("BEAST", "BEASTS ${beasts.count}/${beasts.cap}"),
                SegOption("UNDEAD", "UNDEAD ${undead.count}/${undead.cap}")
            ),
            selectedId = kind.name,
            onSelect = { kind = SupportKind.valueOf(it) }
        )
        group.top3.forEach { RosterRow(it) }
        if (group.top3.isEmpty()) Text(group.emptyText, style = TrueIdleType.body, color = TextMuted)
        MetricLine("TOTAL SUPPORT DPS", group.totalDps)
        TextButton(onClick = { onManage(kind) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("MANAGE", style = TrueIdleType.tag) }
    }
}
```

Also: `MetricLine(label, value)`, `Tag(text, tone)` (glyph prefix for non-color cue), `FeedRow(tag, text, time)`, `SkillRow`, `ConfirmDialog(consequence, confirm, cancel)`, `Sheet`. Components own visuals only; screens own composition.

### 7.6 Combat screen (fixes D16) — order, single column, vertical scroll

```text
┌──────────────────────────────────────────┐
│ ◈ TRUE IDLE · SECTOR 07      🪙 1,250 💀 42│
├──────────────────────────────────────────┤
│ FLOOR 07 · CRYPT SPIDER        [AUTO: ON]│
│ CLEAR 7/10                          70%  │
│ ████████████████░░░░░░░                  │
│ [◄ RETREAT]                [ADVANCE ►]   │
├──────────────────────────────────────────┤
│ HOSTILE · CAVE WARDEN        ELITE  Lv14 │
│ HP      ███████████████████    420/420   │
│ SHIELD  ███████████░░░░░░░░    150/250   │
│ ATK 28 · ARMOR 45 · THREAT HIGH          │
├──────────────────────────────────────────┤
│ CLASH ACTIVE · 142 DPS OUT · 28 DPS IN   │
├──────────────────────────────────────────┤
│ HERO                                Lv12 │
│ HP / SHIELD / MANA gauges                │
│ WEAPON Iron Greatsword           +18 ATK │
│ ARMOR  Steel Plate               +12 DEF │
├──────────────────────────────────────────┤
│ SUPPORT  [BEASTS 2/8 | UNDEAD 4/12]      │
│ DUSK WOLF ★★          16 DPS             │
│ TOTAL SUPPORT DPS               28       │
│ [MANAGE]                                 │
├──────────────────────────────────────────┤
│ DIRECTIVE  UPGRADE TITAN STRENGTH   3/4  │
├──────────────────────────────────────────┤
│ LOG  [ALL][COMBAT][LOOT][SYSTEM]  [CLEAR]│
│ last 3 lines · ALL opens archive         │
└──────────────────────────────────────────┘
```

Rules: hide the SHIELD bar when `maxShield == 0`; boss = hostile panel border `HpRed`, tag `BOSS`, progress replaced by `BOSS ENGAGED`; clash line states `CLASH ACTIVE`, `RECOVERING 4s`, `PAUSED`, `RETREATING`, values `—` when inactive (never stale); log coalesces damage into one line per step; keep the existing category filter (`all/combat/loot/system/gain/milestone`).

### 7.7 Other screens

- **Skills** — header `GOLD · FAITH · POINTS`. Horizontally scrollable `SegmentedTabs`: `OVERVIEW · GATHERING · CRAFTING · BEASTS · NECRO · TRAINING · SANCTIFIED · SLAYER` (the last two are new, fixing D12). Each skill row: name, `Lv n`, XP bar with `cur/next`, effect, next cost, one action. Locked actions show the unmet requirement.
- **Stats** — three sections: base (STR INT VIT LUCK ATK SPD), derived (HP, shield, mana, DPS split hero/beasts/undead), bonuses (gear, dojo, sanctified). Tapping a derived value opens a breakdown sheet generated from `DerivedStats`/`CombatMath` — no separate formulas.
- **Arsenal** — tabs `GEAR · PACK`. Gear: slot rows → compare card (`CURRENT` vs `NEW`, `▲ +6 ATK`). Pack: filter by category, sell with confirmation, lock toggle (Phase 3).
- **System** — Save & Data (export/import §5.3), Offline report (last 5), Notifications (banner toggle, history), Achievements (claim), Diagnostics (version, schema, last save time), Danger Zone (reset: type `RESET` and hold 3 s; lists what is deleted).

### 7.8 Slices and recomposition (fixes D17)

Kotlin 2.1 has strong skipping enabled, but a new `GameState` instance every tick still invalidates every consumer. Screens receive slices:

```kotlin
@Immutable
data class CombatUi(
    val floor: Int, val sector: String, val auto: Boolean, val kills: Int, val killsRequired: Int,
    val phase: Phase, val recoverLeft: Double,
    val enemy: EnemyUi, val hero: HeroUi, val dpsOut: Double, val dpsIn: Double
)

// GameViewModel
val combatUi: StateFlow<CombatUi> = gameState.map { it.toCombatUi() }.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), gameState.value.toCombatUi())
val supportUi: StateFlow<SupportUi> = gameState.map { it.toSupportUi() }.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), gameState.value.toSupportUi())
val resourcesUi: StateFlow<ResourcesUi> = /* gold, bones, ... */
```

Rules: slice classes are `@Immutable` data classes with structural equality; text formatting happens in the slice mapper (or `remember(value)`), through `NumberFormatter`, so unchanged values cost nothing; hidden tabs do not collect; `LazyColumn` for lists > 20 rows with stable `key`s; the log keeps the last 40 lines in state and a derived 3-line preview slice.

```kotlin
// core/NumberFormatter.kt (contract)
object NumberFormatter {
    fun short(x: Long): String        // 999, 1.00K, 12.3K, 123K, 1.23M, 1.23B, 1.23T, then 1.23e15
    fun short(x: Double): String
    fun duration(seconds: Long): String   // 4s · 3m 12s · 2h 05m · 1d 04h
    fun percent(r: Double): String
}
```
3 significant digits, round half-up, carry correctly (`999.9K → 1.00M`), `Locale.ROOT`, invalid input renders `—`. All gold/XP/damage text in the UI goes through it.

---

## 8. Content tables (as they exist; extend, do not scatter)

- Monsters: `Balance.generateMonsterForFloor` — 12 normal names cycling by floor, 8 boss names (add two so floors 90 and 100 are unique, D20). HP `40·1.12^(f−1) + 15f` (boss ×3.5); damage `5·1.09^(f−1) + 2f` (boss ×2.2); shield 0 / 15% (floor > 5) / 30% (boss); armor `1.5f`; gold `5f` (boss ×5); XP `15f` (boss ×4).
- XP curves: skills `1000·lvl³`, slayer `300·lvl³`, player `1830·lvl³`.
- Recipes: smithing — Copper Shortsword (4 Copper, +12), Bronze Breastplate (5 Copper, +8), Iron Claymore (5 Iron, +24), Steel Hauberk (6 Iron, +16); brewing — Minor Health Flask (3 Felweed), Bloodroot Tonic (3 Bloodroot), Moonlight Philter (3 Moonflower). Move to a `Recipes` table with numeric effects.
- Gathering: Felweed 8 s, Bloodroot 12 s, Moonflower 15 s; Copper 5 s (lvl 1), Tin 7 s (lvl 3), Iron 10 s (lvl 5). Vein/plot level requirements are enforced by the engine, not only displayed.
- Minions: Skeleton 10 bones (4 DPS), Bone Mage 25 (8), Dread Knight 50 (16), Bone Lich 100 (35).
- All player-visible strings move to `strings.xml` (with format args) as screens are touched; no new inline literals.
- IDs for items, logs, notifications come only from `Ids.next()` (persisted `meta.nextId`) — never from `System.currentTimeMillis()` (D14).

---

## 9. New systems (after the base is correct)

Each addition requires: a model field, an engine hook, an offline rule, a UI entry, strings, and tests.

1. **Directives.** One active short goal from a rule table over existing counters (`UPGRADE TITAN STRENGTH TO RANK 3`, `CLEAR SECTOR 12`, `HARVEST 5 PLOTS`). Reward: gold or Diamonds. Shown on Combat; tapping opens a sheet with the one-tap action. Completed state persists.
2. **Anomalies.** ≈ 1 per 20 min active; a sheet with two choices and a trade-off (`ROUTE POWER TO SHIELDS` / `SALVAGE THE WRECK`), 60 s timeout → default. Offline: default choice auto-resolved, listed in the offline report (count capped at 6). Uses the engine `Random`.
3. **Evacuation (prestige).** Unlocks at floor 50, previewing shards now vs at +5 floors. Resets floor, player level, gold, dojo ranks, common/uncommon gear, support below ★3, gather/skill levels reset to 1. Keeps: achievements, lifetime stats, Core Shards, rare+ locked gear, ★3+ beasts, settings. Shards `= floor((maxUnlockedFloor / 10.0)^1.5)` (floor 50 → 11, floor 100 → 31). Shard shop (cost `base × 1.6^n`): +% gold, +% XP, +% DPS, offline cap, offline efficiency, starting floor. Floor 100 is the end of a run, not the end of the game.
4. **Codex.** Enemy dossiers unlocked by kill counts, item records, lore line per floor clear; each completed page +0.25% to a stat, total capped at +15%.
5. **Support depth.** 15% damage routing to support units with `WOUNDED` state (§4.7), Undead merge flow, bone-alloy and plating sinks.

---

## 10. Build, manifest, resources (fixes D3, D19)

`app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("debugConfig") {
            val ks = file("${rootDir}/debug.keystore")
            if (ks.exists()) {
                storeFile = ks; storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android"
            }
        }
    }
    buildTypes {
        debug { if (file("${rootDir}/debug.keystore").exists()) signingConfig = signingConfigs.getByName("debugConfig") }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}
dependencies {
    // existing, minus navigation-compose if unused
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:<coroutines version>")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

Add: `app/proguard-rules.pro` (kotlinx.serialization keep rules if R8 reports missing classes), the Gradle wrapper (`gradlew`, `gradle/wrapper/*`) pinned to a version compatible with AGP 8.8.x (or raise AGP to the version Gradle 9.x requires — pick one and make README match), `INTERNET` removed from `main` (keep it in `src/debug/AndroidManifest.xml` only if the AI Studio preview needs it). `metadata.json` is AI Studio scaffolding; leave it untouched.

Backup (saves now exist): `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"` including `files/saves/` and excluding `*.tmp`; restored saves go through the normal load/validate path. Manifest also sets `android:windowSoftInputMode="adjustResize"` for the import text field.

Theme: `Theme.TrueIdle` (parent `android:Theme.Material.NoActionBar`) is fine for Compose; keep the status/navigation bar colors matching `BgDark` (`#000000`, currently `#0B0E14` in `themes.xml` and `colors.xml` — align them).

---

## 11. Accessibility and device matrix

- Widths: 320, 360, 393, 411, 600 dp. Font scale 1.0, 1.3, 1.5, 2.0: rows wrap or grow; no clipped values, no horizontal scroll.
- Insets: `statusBarsPadding` + `displayCutout` on the top bar, `navigationBars` on the bottom bar; scroll content bottom padding = bottom bar height.
- TalkBack: every control has a label; tabs use `Role.Tab` + selected state; `SciFiGauge` exposes `"<label> <value>"` and `progressBarRangeInfo`; decorative glyphs (`◈`, emoji) are not read; live announcements only for `IMPORTANT+` notifications, throttled to 1 per 5 s; combat events never announced.
- Touch targets ≥ 48 dp.
- Contrast ≥ 4.5:1 for text, ≥ 3:1 for components on `SurfaceDark`/`SurfaceRaised`.
- Haptics (`HapticFeedbackType`) on purchase, equip, boss clear; settings switch (`VIBRATE` is already declared).
- Reduced motion: respect `Settings.Global.ANIMATOR_DURATION_SCALE == 0` and an in-app switch; gauges then jump without animation.
- Color-blind safe: `▲/▼` deltas, tag text (`BOSS`, `ELITE`), rarity words.

---

## 12. Tests

Currently none exist. Add under `app/src/test` (JVM) and `app/src/androidTest`.

**JVM unit tests**
- `CombatMathTest`: mitigation curve, shield-then-HP order, incoming floor of 1/s, DPS equals sum of parts.
- `DerivedStatsTest`: defaults give HP 100 / shield 25 / mana 50 / loot 0.35 / tame 0.15; each dojo rank changes exactly its stat; gear swap changes `weaponDmg`/`armorDef`.
- `GameEngineTest`: seeded `Random`; kill grants XP loop (multi-level); hero HP not refilled on kill; recovery state and retreat after 3 failures; floor advance respects `autoAdvance`; boss every 10th floor; notification dedupe keeps distinct messages; ids unique across 10,000 creations.
- `OfflineEngineTest`: parity ±3% vs 2,400 ticks on 20 seeded builds; idempotence (second `resume` awards zero); negative elapsed; reboot cap (10 min); over-cap clamp; claim applies exactly the stored loot.
- `SaveRepositoryTest` (temp dir via `FileSaveStore`): round-trip; truncated file → falls back to `slot.prev`; bit flip → CRC failure; newer schema refused; legacy raw-JSON import migrates; failed self-verification leaves the old slot intact.
- `MigrationsTest`: frozen fixture per schema version.
- `ArchitectureTest`: no `import android.` under `core/`, `engine/`, `data/`.
- `NumberFormatterTest`: every suffix boundary, carry, invalid input.

**Compose UI tests** (existing `testTag`s are reused)
- Width × font-scale matrix: render `CombatScreen` at 320/360/411 dp and scales 1.0/1.5/2.0; assert no `hasVisualOverflow` on tagged value `Text`s (via `SemanticsActions.GetTextLayoutResult`) and no horizontal scroll.
- Bottom bar never overlaps the last scrollable row.
- Tab switch preserves scroll; rotation preserves the selected tab (`rememberSaveable`).
- Beast and Undead rows measure the same height with equal content.
- Confirm flows: reset, sell, import.

**Device / performance**
- Process death while backgrounded → relaunch restores state and shows a correct offline report.
- 50 background/resume cycles: no crash, no double award (`offlineAppliedUntilWallMs` strictly increases).
- Airplane-mode session of 30 min.
- JankStats in debug; Macrobenchmark for startup and 5 min of combat; Baseline Profile generated for release.

---

## 13. Roadmap and acceptance gates

**Phase 0 — Builds from a clean clone** (D3, D19)
Committed wrapper; conditional signing; `proguard-rules.pro`; README tooling versions match reality; `INTERNET` handled; `rememberSaveable` tab (D18).
*Gate:* `./gradlew :app:assembleDebug :app:assembleRelease` succeeds on a fresh clone.

**Phase 1 — Correctness core** (D1, D2, D4, D5, D6, D7, D9, D10, D11, D13, D14)
`GameClock`, `core/` (`CombatMath`, `DerivedStats`, `NumberFormatter`, `Ids`), lifecycle-aware loop, `SaveRepository` + `FileSaveStore` + migrations, `OfflineEngine`, recovery state, derived stats, numeric gear values, notification fix, balance retune for armor mitigation.
*Gate:* JVM test suite of §12 passes; kill → process death → relaunch restores state; offline parity ±3%; repeated resume awards zero.

**Phase 2 — Readable UI** (D15, D16, D17)
`TrueIdleType` (12 sp floor), `SciFiPanel`/`SciFiGauge`/`SegmentedTabs`/`SupportModule`/`RosterRow`, stacked Combat screen, bottom bar semantics, slices in the ViewModel, SAF export/import, offline dialog fed by `OfflineLoot.items`.
*Gate:* no text below 11 sp (grep `fontSize = [0-9]\.sp` returns nothing); matrix test passes at 320 dp / 1.5 font scale; Compose recomposition counts for Combat ≤ 4 per second.

**Phase 3 — Complete the 10 skills** (D12, D20)
Slayer, Sanctified, achievements with counters and claim, Diamonds source/sinks, gather/skill level effects and requirements enforced, auto-collect, support damage routing, boss name fix.
*Gate:* every field in `GameState` is read and written by the engine or removed; achievements progress and pay out; save → load → identical `GameState`.

**Phase 4 — Depth** (§9)
Directives, anomalies, Evacuation prestige and shard shop, codex, Undead merge flow.
*Gate:* a seeded greedy simulation (`src/test`) reaches floor 10 / 30 / 50 / first prestige within ±10% of recorded milestones; no gap longer than 5 min before floor 50 without an affordable purchase or directive.

Release gate (end of Phase 2 and each later phase): clean build, all tests green, migration from every previous schema loads, budgets met on a mid-range device (engine tick ≤ 1.5 ms p50 / 4 ms p95, heap ≤ 60 MB after 30 min, janky frames < 2%, battery ≤ 3% per 30 min foreground, save write ≤ 40 ms, offline batch ≤ 300 ms).

---

## 14. Final rule

Every visible line answers one question: what is happening, how much remains, what changed, or what can be done. If a panel cannot do that at 12 sp on a 320 dp screen, redesign the panel instead of shrinking the text.
