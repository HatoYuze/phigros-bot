package com.github.hatoyuze

import com.github.hatoyuze.mirai.command.PhiCommand
import com.github.hatoyuze.mirai.data.*
import com.github.hatoyuze.mirai.data.GithubDataUpdater.Companion.IllustrationUpdater
import kotlinx.coroutines.runBlocking
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
        GlobalAliasLibrary.reload().also {
            if (GlobalAliasLibrary.alias.isEmpty()) {
                PhigrosBot.getResource("phi/alias.json")!!.let { Json.decodeFromString<MutableMap<String, MutableSet<String>>>(it) }.forEach { (k, v) ->
                    GlobalAliasLibrary.alias[k] = v
                }
            }
            logger.info("成功获取 ${GlobalAliasLibrary.alias.values.sumOf { it.size }} 个别名！")
        }
        GithubDownloadProxy.reload()
        logger.info("更新配置文件成功")

        logger.info("正在检查曲绘更新列表...")
        runBlocking {
            IllustrationUpdater.update()
        }
        logger.info("更新曲绘成功")
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