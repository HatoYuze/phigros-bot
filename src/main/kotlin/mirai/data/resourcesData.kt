package com.github.hatoyuze.mirai.data

import com.github.hatoyuze.PhigrosBot
import kotlinx.serialization.Serializable


@Serializable
data class PhigrosSongData(
    val sid: String,
    val title: String,
    val bpmDescription: String,
    val illustrator: String,
    val composer: String,
    val source: String,
    val previewTimeFrom: Double,
    val previewTimeTo: Double,
    val chapter: String,
    val charts: List<PhigrosSongChartData>
) {
    fun addAlias(newAlias: String) = GlobalAliasLibrary.alias.compute(sid) { _, v ->
        v?.add(newAlias)
        v ?: mutableSetOf(newAlias)
    }
    fun queryAliases() = GlobalAliasLibrary.alias[sid] ?: mutableSetOf()

    fun getIllustration() =
        PhigrosBot.dataFolder.resolve("Illustration").resolve("$sid.png")
            .also { if (!it.exists()) throw IllegalStateException("没有找到歌曲 $sid 对应的曲绘") }

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
    val charter: String,
    val notes: Int
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
