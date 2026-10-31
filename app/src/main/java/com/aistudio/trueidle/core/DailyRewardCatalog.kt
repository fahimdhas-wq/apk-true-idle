package com.aistudio.trueidle.core

data class DailyReward(
    val day: Int,
    val gold: Long = 0L,
    val diamonds: Long = 0L,
    val faith: Long = 0L,
    val bones: Long = 0L,
    val potionName: String = "",
    val potionCount: Int = 0,
    val title: String = ""
)

object DailyRewardCatalog {
    /** 7-Day Sprint Track focused on massive Gold and Diamonds. */
    val SPRINT_7_REWARDS = listOf(
        DailyReward(day = 1, gold = 50_000L, diamonds = 25L, title = "Vanguard Starter"),
        DailyReward(day = 2, gold = 150_000L, diamonds = 60L, title = "Recon Bounty"),
        DailyReward(day = 3, gold = 400_000L, diamonds = 120L, title = "War Chest"),
        DailyReward(day = 4, gold = 1_000_000L, diamonds = 250L, title = "Sector Cache"),
        DailyReward(day = 5, gold = 2_500_000L, diamonds = 450L, title = "Strike Fund"),
        DailyReward(day = 6, gold = 6_500_000L, diamonds = 750L, title = "Armory Reserve"),
        DailyReward(day = 7, gold = 20_000_000L, diamonds = 1_500L, title = "Apex Grand Jackpot")
    )

    /** 30-Day Monthly Track with rich Gold, Diamonds, Faith, Bone Essence, and Potions. */
    val CALENDAR_30_REWARDS: List<DailyReward> = (1..30).map { day ->
        when (day) {
            7 -> DailyReward(day = 7, gold = 10_000_000L, diamonds = 500L, faith = 1_000L, bones = 1_000L, potionName = "Vitality Elixir", potionCount = 5, title = "Week 1 Cache")
            14 -> DailyReward(day = 14, gold = 25_000_000L, diamonds = 1_000L, faith = 2_500L, bones = 2_500L, potionName = "Moonshade Concoction", potionCount = 5, title = "Fortnight Treasury")
            21 -> DailyReward(day = 21, gold = 50_000_000L, diamonds = 2_000L, faith = 5_000L, bones = 5_000L, potionName = "Sacred Lotus Ambrosia", potionCount = 5, title = "Three-Week Bounty")
            30 -> DailyReward(day = 30, gold = 150_000_000L, diamonds = 5_000L, faith = 15_000L, bones = 15_000L, potionName = "Worldtree Dew", potionCount = 10, title = "Grandmaster Trove")
            else -> {
                val gold = 300_000L * day
                val dia = (15L + day * 6L)
                val faith = if (day % 3 == 0) 250L * day else 0L
                val bones = if (day % 2 == 0) 200L * day else 0L
                val potion = when (day % 5) {
                    0 -> "Minor Health Flask" to 4
                    1 -> "Mana Infusion Tonic" to 4
                    2 -> "Cleansing Antidote" to 3
                    3 -> "Vitality Elixir" to 3
                    else -> "Spirit Grass Philter" to 3
                }
                DailyReward(
                    day = day,
                    gold = gold,
                    diamonds = dia,
                    faith = faith,
                    bones = bones,
                    potionName = potion.first,
                    potionCount = potion.second,
                    title = "Day $day Supply"
                )
            }
        }
    }
}
