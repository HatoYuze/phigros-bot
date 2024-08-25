package com.github.hatoyuze

import com.github.hatoyuze.mirai.data.GlobalUserData
import com.github.hatoyuze.mirai.command.PhiCommand
import com.github.hatoyuze.mirai.data.AliasLibrary
import com.github.hatoyuze.mirai.data.PhigrosSongData
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin

object PhigrosBot: KotlinPlugin(
    JvmPluginDescription(
        id = "com.github.hatoyuze",
        name = "phigros-bot",
        version = "0.1.0"
    ) {
        author("HatoYuze & Rosemoe")
    }
) {
    override fun onEnable() {
        super.onEnable()
        CommandManager.registerCommand(PhiCommand)
        GlobalUserData.reload()
        AliasLibrary.reload().also { AliasLibrary.alias }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val SONGS_DATABASE: List<PhigrosSongData> by lazy {
        json.decodeFromString<List<PhigrosSongData>>(
            getResource("phi/music-info.json")!!
        )
    }
}