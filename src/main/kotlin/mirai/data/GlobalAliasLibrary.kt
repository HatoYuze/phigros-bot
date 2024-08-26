package com.github.hatoyuze.mirai.data

import com.github.hatoyuze.PhigrosBot
import com.github.hatoyuze.PhigrosBot.SONGS_DATABASE
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.similarity.LevenshteinDistance

object GlobalAliasLibrary : AutoSavePluginConfig("aliases") {
    @ValueDescription("用于调整曲名匹配的灵敏度\n数值越小则代表匹配时越为灵敏")
    private val sensitivity by value<Double>(0.4)

    @ValueDescription("是否可以使用指令添加别名")
    val isNormalUserAllowed by value(true)

    val alias by value<MutableMap<String, MutableSet<String>>> {
        PhigrosBot.getResource("phi/alias.json")
            ?.let { Json.decodeFromString<MutableMap<String, MutableSet<String>>>(it) }
            ?: mutableMapOf<String, MutableSet<String>>().also { PhigrosBot.logger.error("Can not access the alias library in the resources") }
    }



    fun searchSongWithAlias(name: String): List<PhigrosSongData> {
        return SONGS_DATABASE.filter {
            if (name == it.sid) return listOf(it)
            val aliases = alias[it.sid] ?: return@filter it.title == name
            name in aliases ||
                matchRateByLevenshtein(it.title, name) // Note: sid 就是 title 多了曲师的描述，这里就不用追加比较了
        }
    }

    private fun matchRateByLevenshtein(origin: String, target: String): Boolean {
        fun String.ignoreAccents(): String = StringUtils.stripAccents(this.replace('Ø','O'))
        fun String.ignoreSpecialCharacters(): String = filter { Character.isIdentifierIgnorable(it.code) || it != ' ' }
        fun String.simpleText() = ignoreAccents().ignoreSpecialCharacters()


        return LevenshteinDistance.getDefaultInstance().apply(origin.simpleText(), target.simpleText()) / origin.length.toDouble() < sensitivity
    }

}