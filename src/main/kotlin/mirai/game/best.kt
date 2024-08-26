package com.github.hatoyuze.mirai.game

import com.github.hatoyuze.mirai.data.get
import com.github.hatoyuze.mirai.data.PhigrosLevel.Companion.id
import com.github.hatoyuze.mirai.data.PhigrosLevel.Legacy
import com.github.hatoyuze.mirai.data.PhigrosSongChartData
import com.github.hatoyuze.mirai.data.PhigrosSongData
import com.github.hatoyuze.mirai.data.PhigrosSongData.Companion.getSongDataBySid
import com.github.hatoyuze.protocol.api.PhigrosUser
import com.github.hatoyuze.protocol.data.SaveOwner
import com.github.hatoyuze.protocol.data.SavePlayScore
import com.github.hatoyuze.protocol.data.Score
import com.github.hatoyuze.protocol.data.Summary
import java.time.LocalDateTime
import kotlin.math.pow


data class GameSave(
    val summary: Summary,
    val user: SaveOwner,
    val scores: SavePlayScore,
    val updateTime: LocalDateTime,
    val playerName: String,
    val userId: String,
) {
    companion object{
        fun new(phigrosUser: PhigrosUser) =
            GameSave(
                phigrosUser.userSummary,
                phigrosUser.userInfo,
                phigrosUser.playScore,
                phigrosUser.lastUpdateTime,
                phigrosUser.userName,
                phigrosUser.userInternalId,
            )
    }
}


data class PhigrosBest19Result(
    val userNick: String,
    val rks: Double,
    val best19: List<BestItem>,
    val phi1: BestItem?,
    val bestExtended3: List<BestItem>,
    val phiExtended3: List<BestItem>
) {
    data class BestItem(
        val score: Score,
        val rating: Double,
        val song: PhigrosSongData,
        val chart: PhigrosSongChartData
    ) {

        override fun toString(): String {
            return "【${song.title}】 ${chart.level}(${chart.rating}) -> ${rating.stringWithPoint(2)} | ${score.score.toString().padStart(7,'0')} (${score.achievement.stringWithPoint(2)}%) ${getRank(score.score)}"
        }
    }
    companion object {
        fun Double.stringWithPoint(point: Int = 2) = "%.${point}f".format(this)

        //from -> https://phigros.fandom.com/zh/wiki/Phigros_Wiki#%E8%AF%84%E7%BA%A7
        fun getRank(score: Int) = when(score) {
            100_0000 -> "φ"
            in 96_0000 until 100_0000 -> "V"
            in 92_0000 until 96_0000 -> "S"
            in 88_0000 until 92_0000 -> "A"
            in 82_0000 until 88_0000 -> "B"
            in 70_0000 until 82_0000 -> "C"
            else -> "F"
        }
    }
}

object PhigrosTool{
    fun getRankingScore(constant: Double,achievement: Double): Double {
        val factor = ((achievement - 55) / 45).pow(2)
        return factor * constant
    }
    private fun computeBests(save: GameSave): List<PhigrosBest19Result.BestItem> {
        val result = mutableListOf<PhigrosBest19Result.BestItem>()
        for (score in save.scores.scores) {
            if (score.levelIdx == Legacy.ordinal) {
                continue
            }
            val song = getSongDataBySid(score.sid) ?: continue
            val chart = song.charts[id(score.levelIdx)]
            result.add(
                PhigrosBest19Result.BestItem(
                    score,
                    getRankingScore(chart.rating, score.achievement),
                    song,
                    chart
                )
            )
        }
        result.sortBy { -it.rating }
        return result
    }
    /**
     * Mapping to [List.subList]
     * */
    private fun <T> List<T>.subListOrEmpty(fromIdx: Int, toIndex: Int): List<T> {
        val result = mutableListOf<T>()
        if (lastIndex <= fromIdx) return emptyList()
        for (i in fromIdx until toIndex) {
            val value = getOrNull(i) ?: break
            result.add(value)
        }
        return result
    }

    fun getB19(phigrosUser: PhigrosUser): PhigrosBest19Result {
        val gameSave = GameSave.new(phigrosUser)
        val bests = computeBests(gameSave)
        val phiList  = bests.filter { it.score.score == 100_0000 && it.score.achievement == 100.0 }.sortedBy { -it.rating }
        val phi1 = phiList.firstOrNull()
        val best19 = bests.take(19)
        val newRks = (best19.sumOf { it.rating } + (phi1?.rating ?: 0.0)) / 20
        return PhigrosBest19Result(
            gameSave.playerName,
            newRks,
            best19 = bests.subListOrEmpty(0,18),
            phi1 = phiList.firstOrNull(),
            bestExtended3 = bests.subListOrEmpty(19, 23),
            phiExtended3 = phiList.subListOrEmpty(1,4)
        )
    }
}

