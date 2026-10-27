package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The single telemetry/log-export builder: the Settings export buttons, the ViewModel and LogView
 * all render the same dossier, so the three previously duplicated implementations cannot drift.
 * ASCII only, so a shared log stays readable in every client.
 */
object Telemetry {

    /** Full report over the most recent log lines. */
    fun report(state: GameState): String = report(state, state.logs.takeLast(MAX_LOG_LINES))

    /** Report over an explicit log slice (LogView exports the currently filtered feed). */
    fun report(state: GameState?, logs: List<LogEntry>): String {
        val sb = StringBuilder()
        rule(sb)
        line(sb, "TRUE IDLE // TACTICAL MISSION TELEMETRY EXPORT")
        rule(sb)
        if (state != null) {
            val stats = DerivedStats.of(state)
            line(sb, "MISSION DOSSIER:")
            line(sb, "- Active Play Time   : ${NumberFormatter.duration(state.lifetime.playTimeSeconds)}")
            line(sb, "- Current Sector     : Sector ${state.player.currentFloor} (Max Cleared: ${state.player.maxUnlockedFloor})")
            line(sb, "- Combat Level       : Level ${state.player.lvl} (${state.player.xp} XP)")
            line(sb, "- Gear Score         : ${stats.gearScore} GS")
            line(sb, "- Total Eliminations : ${state.lifetime.totalKills}")
            line(sb, "- Total Credits      : ${state.player.gold}")
            line(sb, "- Bone Essence       : ${state.necro.boneEssence}")
            line(sb, "- Holy Faith         : ${state.essence.faith}")
            line(sb, "- Slayer Points      : ${state.slayer.slayerPoints} PTS")
            line(sb, "- Companions Tamed   : ${state.beasts.beasts.size}/${GameCaps.BEAST}")
            line(sb, "- Undead Army        : ${state.necro.army.sumOf { it.count }}/${GameCaps.MINION}")
            line(sb, "- Promotion Tier     : Tier ${state.promotion.tier} (${state.promotion.tierName})")
        }
        rule(sb)
        line(sb, "EVENT STREAM ARCHIVE (${logs.size} EVENTS):")
        rule(sb)
        logs.forEach { sb.append(formatLine(it)) }
        rule(sb)
        line(sb, "END OF TELEMETRY EXPORT")
        return sb.toString()
    }

    private fun formatLine(entry: LogEntry): String {
        val time = if (entry.timestampMs > 0L) {
            SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date(entry.timestampMs))
        } else {
            "--:--:--"
        }
        return "[$time] [${entry.type.uppercase()}] ${entry.text}\n"
    }

    private fun rule(sb: StringBuilder) = line(sb, "=".repeat(RULE_WIDTH))

    private fun line(sb: StringBuilder, text: String) {
        sb.append(text).append('\n')
    }

    private const val RULE_WIDTH = 64
    private const val TIME_PATTERN = "HH:mm:ss"
    private const val MAX_LOG_LINES = 100
}
