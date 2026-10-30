# True IDLE — Complete Changelog

**Release:** True IDLE (Latest Release)  
**Platform:** Android  
**Build:** Kotlin · Jetpack Compose · Material 3 · Gradle  
**Changelog coverage:** Initial Android release through October 30, 2026

This release consolidates the game's core systems, expanded content, progression rebalance, interface work, save reliability, performance improvements, and automated Android build pipeline.

## 1. Core game and combat

- Native Android idle-RPG implementation using Kotlin and Jetpack Compose.
- Persistent game loop running at four simulation ticks per second (250 ms per tick).
- Dungeon progression across 100 floors, including boss milestones, auto-advance, and floor navigation.
- Combat state covering health, shields, mana, attack and defence calculations, enemy state, rewards, and combat logs.
- Batch processing for high-speed combat so multiple kills can be resolved together rather than requiring one full processing pass per kill.
- Centralized combat calculations and derived-stat computation.
- Cached derived stats and reduced repeated timer / stat work in the game loop.
- Combat telemetry and cleaner combat presentation, including removal of duplicated DPS information.
- Floor, monster, kill-count, and offline-resume state handling.

## 2. Player progression, stats, and skills

- Interconnected skill and progression systems, including Combat, Farming, Mining, Smithing, Herblore, Training, Slayer, Genesis, Essence, and Talents.
- Player stat progression for Strength, Intelligence, Vitality, Luck, and attack speed.
- Gold-funded training nodes and permanent stat bonuses.
- Promotion and gear-score progression with ordered tier ranks.
- Skill dashboard with dedicated progression bars, category summaries, status indicators, and direct navigation to each subsystem.
- Shared XP requirement curves, with gathering progression separated from ordinary skill progression.

## 3. Farming and Mining — resource content and unlocks

- Twenty farming resources and twenty mining resources maintained in a canonical item catalog.
- Farming and Mining have separate levels, XP banks, resource lists, automation queues, and item proficiency.
- Resource access is gated by the relevant gathering skill level.
- A new plot or vein unlocks every five Farming or Mining levels, following the current resource schedule.
- Locked resources are visibly identified and cannot be harvested, mined, or added to automation queues before their required level.
- Existing saves are reconciled with the current resource catalog and retain previously unlocked progression where valid.
- Farming and Mining speed and yield upgrades are integrated with the resource loop.
- Manual gathering and automated gathering use the same reward calculations.

## 4. Harvest proficiency and yield progression

- Each individual farming plot and mining vein has its own proficiency level and XP.
- Proficiency progression uses the dedicated high-growth exponent-7 curve.
- Every proficiency level doubles the resource yield: 1x, 2x, 4x, 8x, and onward, capped at proficiency level 100.
- Proficiency XP is awarded from the actual proficiency-adjusted quantity collected, not a fixed one-point reward.
- Explicit yield upgrades are included in the final quantity used for rewards.
- Proficiency XP addition is protected against negative values and Long overflow.
- Proficiency XP banks are cleared at the maximum level so capped resources cannot accumulate an unbounded overflow bank.
- Automation queue capacity scales with gathering level: one additional queue slot per ten skill levels.
- Queue toggles enforce the resource unlock requirement and available queue capacity.

## 5. Tier-weighted gathering XP and large-number progression

- Added a central Harvest XP table that assigns XP per item according to the resource tier.
- Gathering skill XP is calculated from actual quantity collected multiplied by XP per item for that resource tier.
- Tier-one resources award 12 XP per item; each successive tier adds another 12 XP per item.
- Examples: 1 Stone = 12 XP; 3 Stone = 36 XP; 4 Stone = 48 XP; 3 Coal = 72 XP.
- Large quantities retain their scale: a 1F-sized tier-one item quantity produces 12F gathering XP.
- Manual harvest, manual mining, queued automation, and auto-collection share the same tier-weighted XP calculation.
- The Harvest screen previews the skill XP and proficiency XP expected from each resource collection.
- Farming and Mining use Double XP storage and dedicated large-number requirement curves so gathering rewards are not truncated at Long.MAX_VALUE.
- Farming and Mining each have a dedicated level-100 cap.
- Gathering XP progress bars use a Double calculation path and display a maximum-level state.
- The progression curve is calibrated against an explicitly defined optimized maximum-proficiency production simulation. Its roughly 4.5-year estimate is a theoretical continuous-output model, not a promise of ordinary play time.

## 6. Equipment, inventory, crafting, and economy

- Expanded equipment system with 13 distinct equipment slot types.
- Centralized item and resource identifiers for consistent inventory, equipment, crafting, and rewards.
- Equipment stat bonuses flow through the derived-stat system.
- Expanded mining ore and farming herb catalogs.
- Forge and Smithing progression with additional equipment tiers, including the expanded tiers four through six.
- Herblore and alchemy recipes for restorative and mana-related potions.
- Potion and crafting recipes use shared catalog definitions so UI prices and engine resource costs agree.
- Upgrade prices are calculated through one shared upgrade-cost table.
- Inventory presentation supports large quantities and consistent item categories.
- Legacy item identifiers and existing-save reconciliation are covered by migration and compatibility tests where applicable.

## 7. Genesis — beasts and reanimation

- Beast capture and companion progression.
- Beast leveling and star-rank merging.
- Expanded beast content and shared tier/rank styling.
- Necromancy / reanimation progression using bone essence.
- Multiple summonable reanimation units, including Skeletons, Bone Mages, Dread Knights, and Bone Liches.
- Beast and reanimation contributions are included in the game's derived combat power and DPS calculations.

## 8. Essence, Aegis, and combat talents

- Essence progression and Faith resource tracking.
- Aegis barrier activation, absorption, and Faith drain.
- Smite upgrades and centralized barrier / combat bonus calculations.
- Previously purchasable but ineffective Essence upgrades are wired into the actual engine calculations.
- Active talents and passive Struggle progression.
- Struggle bonuses use tracked thresholds and spent talent points, with the descriptions and calculations aligned.
- Fractional remainders are retained for Faith drain and struggle tracking so small per-tick progress is not discarded.

## 9. Slayer, achievements, and rewards

- Slayer task progression, targets, kill tracking, and Slayer points.
- Achievement evaluation and reward handling.
- Boss first-clear diamond rewards are surfaced in the UI.
- Offline level-up reporting includes the progression achieved while away.
- No-op achievement checks are skipped when there is no relevant state change.

## 10. Offline progression and background execution

- Dedicated offline simulation path for elapsed time away from the game.
- Offline rewards account for combat and supported non-combat systems.
- Offline reports are shown in a dedicated dialog with gained resources and level-ups.
- Offline report threshold was reduced from five minutes to five seconds.
- Background execution can be enabled through a Settings control using an Android foreground service.
- Background execution includes a confirmation / caution step and live CPU/RAM and estimated battery-cost information.
- Resume accounting uses the last active elapsed time so already simulated time is not paid again as offline progress.
- Pending offline reports are merged safely and live simulation pauses while the report is waiting to be claimed.
- Eight-hour idle alerts are scheduled as actual alarms rather than being raised only when the app returns to the foreground.
- Notification history and notification importance filtering are supported.

## 11. Save system, recovery, and compatibility

- Persistent game state stored through a centralized save repository.
- Save export and import support.
- Save envelopes include a format identifier and CRC32 integrity check.
- Stepwise save migrations operate on the raw JSON payload.
- Legacy saves receive newly added content through reconciliation without duplicating already-owned entries.
- Loaded state is clamped and sanitized before it enters the game engine.
- Unreadable save slots are quarantined behind a recovery flow rather than silently overwritten.
- Save writes are debounced and serialized through a single IO writer.
- Integer-valued legacy Farming and Mining XP fields remain compatible with the new Double storage.
- Added coverage for large gathering XP serialization and legacy integer migration.
- Unknown or unsupported future save schemas are rejected rather than silently downgraded.

## 12. Large-number formatting and tier configuration

- One shared BigNumberFormatter is the canonical number-rendering engine used across the game.
- Alphabetic suffix notation supports A through Z, then AA, AB, and further combinations without a fixed final letter.
- Default suffix magnitudes are configurable; the shipped alphabetic mode starts at 10^12 and advances by three exponents per suffix.
- The original compact K/M/B/T, grouped-number, and scientific-notation modes remain available.
- Formatting configuration is centralized and persisted in the save model.
- Large values are handled through BigDecimal / BigInteger formatting paths rather than converting them to Float scratch values.
- Formatting uses explicit rounding and locale-independent output.
- A fixed 2,048-entry formatting cache prevents unbounded cache growth.
- Configuration changes invalidate cached strings so old notation is not reused.
- Stable displayed-number components avoid reformatting unchanged values.
- One canonical TierConfig defines tier order, rank, and colors.
- Tier progression is explicitly ordered from F-, F, F+ and onward through the extended high-tier ranks; rank is never inferred from alphabetical sorting or color.
- Tier colors are parsed once and reused as integer color values, avoiding repeated hex parsing during UI updates.
- Unknown tier identifiers resolve safely to an explicit fallback.

## 13. User interface and accessibility

- High-contrast sci-fi / cyberpunk interface with dark surfaces, neon accents, structured panels, and Orbitron typography.
- Shared Compose components for panels, controls, gauges, dialogs, telemetry, tabs, badges, and XP progress bars.
- Global UI scaling through the Compose theme.
- System font scaling is respected and dialog density is preserved when Android opens a separate dialog window.
- Settings rebuilt with consistent panel spacing, dividers, and touch-friendly controls.
- Log filters use a full-width layout; Copy Log and Clear Log have their own unclipped row.
- Inventory category targets and sub-tab controls were enlarged.
- Interactive targets were raised to accessible touch sizes where needed; tab semantics and decorative glyph handling were improved.
- Numeric text and XP bars are bounded so large values do not push labels off narrow screens.
- Duplicate panels and excess vertical content were removed from the combat and stats screens.
- Tab and selection state is preserved through configuration changes where appropriate.
- Buttons use consistent controls and avoid decorative emoji glyphs.

## 14. Runtime performance and engineering

- Derived stats are cached per relevant game state instead of being repeatedly recalculated on every recomposition.
- Combat processing supports batch kills for high-speed progression.
- Timer and no-op update work is pruned from the main loop.
- Large-number formatting uses a bounded cache and stable display values.
- Shared catalogs, balance tables, tier metadata, and upgrade-cost formulas reduce duplicated logic.
- Save IO is kept off the gameplay loop and serialized through one writer.
- A benchmark test harness and JVM unit tests cover core progression and engine behavior.
- ProGuard rules protect serialization models during release shrinking.
- A baseline profile is included for Android runtime startup / UI execution optimization.
- Compose architecture boundaries and core-module dependencies are checked by tests.

## 15. Android build and release pipeline

- Native Android application built with Kotlin, Jetpack Compose, and Material 3.
- Android SDK target 36, minimum SDK 26, and JDK 21.
- Committed Gradle wrapper standardizes local and CI builds.
- Debug signing configuration is generated / configured for CI builds.
- CI explicitly makes gradlew executable before invoking Gradle.
- GitHub Actions runs unit tests, Android lint, debug APK assembly, and release APK assembly.
- APK artifacts are uploaded from successful CI runs.
- Successful main-branch and version-tag builds publish or update the GitHub Release and attach both APK variants.

## 16. Verification included in this release

- Unit tests passed on the release commit.
- Android lint passed on the release commit.
- Debug APK assembly passed.
- Release APK assembly passed.
- APK upload and GitHub Release publication passed.

---

## Download

- **Debug:** app-debug.apk
- **Release:** app-release.apk

For source code, issue tracking, and the complete commit history, see the [True IDLE repository](https://github.com/fahimdhas-wq/apk-true-idle).
