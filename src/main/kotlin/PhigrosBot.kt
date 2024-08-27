package com.github.hatoyuze

import com.github.hatoyuze.mirai.command.PhiCommand
import com.github.hatoyuze.mirai.data.*
import com.github.hatoyuze.mirai.data.GithubDataUpdater.Companion.IllustrationUpdater
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import java.io.File

object PhigrosBot: KotlinPlugin(
    JvmPluginDescription(
        id = "com.github.hatoyuze.phigros-bot",
        name = "phigros-bot",
        version = "0.3.0"
    ) {
        author("HatoYuze & Rosemoe")
    }
) {
    private fun migrateConfigDataId() {
        fun moveFiles(origin: Array<File>) {
            for (dataFile in origin) {
                if (dataFile.isDirectory) {
                    moveFiles(dataFile.listFiles()!!)
                    dataFile.delete()
                    continue
                }
                val target = dataFolder.resolve(dataFile.name)
                if (target.exists() && target.length() > dataFile.length()) continue
                dataFile.copyTo(target, true)
                dataFile.delete()
            }
        }
        val data = dataFolder.parentFile.resolve("com.github.hatoyuze")
        if (!data.exists()) {
            return
        }
        logger.info("发现了残余的旧配置文件, 正在迁移中...")
        val config = configFolder.parentFile.resolve("com.github.hatoyuze")
        dataFolder.mkdirs()
        config.mkdirs()
        moveFiles(data.listFiles()!!).also { data.delete() }
        moveFiles(config.listFiles()!!).also { config.delete() }
        logger.info("迁移文件成功！")
    }

    override fun onEnable() {
        super.onEnable()
        migrateConfigDataId()
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
            getResource("phi/song_info.json")!!
        )
    }
}