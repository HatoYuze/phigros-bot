package com.github.hatoyuze.mirai.data

import com.github.hatoyuze.PhigrosBot
import kotlinx.serialization.Serializable


@Serializable
data class PhigrosSongData(
    val sid: String,
    val title: String,
    val illustrator: String,
    val composer: String,
    val previewTimeFrom: Double,
    val previewTimeTo: Double,
    val charts: List<PhigrosSongChartData>
) {
    companion object{
        fun getSongDataBySid(sid: String): PhigrosSongData? {
            return PhigrosBot.SONGS_DATABASE.find { it.sid == sid }
        }
        fun getSongDataByTitle(title: String): PhigrosSongData? {
            return PhigrosBot.SONGS_DATABASE.find { it.title.equals(title,true) }
        }
    }
}

operator fun List<PhigrosSongChartData>.get(level: PhigrosLevel) =
    find { it.level == level } ?: throw IllegalArgumentException("Level $level is invalid in $this")

@Serializable
data class PhigrosSongChartData(
    val level: PhigrosLevel,
    val rating: Double,
    val charter: String
)

@Serializable
enum class PhigrosLevel{
    EZ,
    HD,
    IN,
    AT,
    Legacy;
    companion object {
        fun id(idx: Int) = PhigrosLevel.entries[idx]
    }
}
