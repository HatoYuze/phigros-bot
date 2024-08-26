package com.github.hatoyuze.mirai.data

import com.github.hatoyuze.PhigrosBot
import com.github.hatoyuze.mirai.data.GithubDownloadProxy.GithubSetting.PREFIX
import com.github.hatoyuze.mirai.data.GithubDownloadProxy.GithubSetting.REPLACE
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.utils.MiraiLogger
import java.io.File

object GithubDownloadProxy : AutoSavePluginConfig("github_proxy_url_setting") {
    enum class GithubSetting {
        PREFIX,
        REPLACE
    }

    @ValueDescription("是否启用 raw.githubusercontent.com 请求链接替换功能")
    private val enable by value(true)

    @ValueDescription("替换的模式\nPREFIX 指在原链接前直接添加前缀 url\nREPLACE 指替换原链接的 raw.githubusercontent.com 为指定 url")
    private val method: GithubSetting by value(PREFIX)
    private val url: String by value("https://gh.api.99988866.xyz/")

    fun newProxyUrl(githubUrl: String): String {
        if (!enable) return githubUrl
        return when (method) {
            PREFIX -> url + githubUrl
            REPLACE -> url.replace("raw.githubusercontent.com", url)
        }
    }
}

class GithubDataUpdater private constructor(
    private val localDownloadFile: File,
    val pathOnline: String,
    branch: String = "main"
) {
    private val githubApiUrl = "https://api.github.com/repos/hatoyuze/phigros-bot/contents/$pathOnline?ref=$branch"

    @Serializable
    data class GithubFileContent(
        val name: String,
        val sha: String,
        val size: Int,
        @SerialName("download_url")
        val downloadUrl0: String
    ) {
        val downloadUrl by lazy { GithubDownloadProxy.newProxyUrl(downloadUrl0) }
    }


    private suspend fun fetchRepositoryFiles() =
        http.get(githubApiUrl).body<List<GithubFileContent>>()

    private suspend fun getOutdatedFiles(): List<GithubFileContent> {
        val local = localDownloadFile.also { it.mkdirs() }
        val files = local.listFiles()!!
        val repoList = fetchRepositoryFiles()
        val result = mutableListOf<GithubFileContent>()
        for (repoFile in repoList) {
            val file = files.find { it.name == repoFile.name }
            if (file == null || file.length() != repoFile.size.toLong()) {
                result.add(repoFile)
                continue
            }
        }
        return result
    }


    suspend fun update() {
        val list = getOutdatedFiles()
        if (list.isEmpty()) {
            logger.info("没有找到需要更新的 $pathOnline 列表! ")
            return
        }
        list.chunked(5)
            .map { launchDownloadImpl(it) }
            .onEach { it.join() }
    }

    private inline fun handleException(errorMessage: String, block: () -> Unit) {
        kotlin.runCatching {
            block.invoke()
        }.onFailure {
            logger.error("ERROR ${it.javaClass.name}: ${it.message}")
            kotlin.runCatching {
                block.invoke()
            }.onFailure { newE ->
                logger.error(errorMessage)
                logger.error(newE)
            }
        }
    }

    private fun launchDownloadImpl(groupResp: List<GithubFileContent>) =
        coroutineContext.launch {
            groupResp.onEach { content ->
                handleException("下载 ${content.name} 图片失败 -> ${content.downloadUrl}") {
                    val resp = http.get(content.downloadUrl)
                    if (resp.status != HttpStatusCode.OK) {
                        error("Http status error: ${resp.status} to ${resp.call.request.url}")
                    }
                    val bytes = resp.readBytes()
                    if (bytes.size != content.size) {
                        error("The data is truncated! -> ${content.downloadUrl}")
                    }
                    val outputFile = localDownloadFile.resolve(content.name).apply { createNewFile() }
                    outputFile.writeBytes(bytes)
                    logger.info("成功更新 $pathOnline -> ${content.name} (${content.size} bytes)")
                }
            }
        }

    companion object {
        private val http = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
                install(HttpTimeout) {
                    socketTimeoutMillis = 30_000
                    connectTimeoutMillis = 30_000
                    requestTimeoutMillis = 60_000
                }
            }
        }

        private val coroutineContext = PhigrosBot

        private val logger by lazy {
            MiraiLogger.Factory.create(GithubDataUpdater::class)
        }

        val IllustrationUpdater by lazy {
            GithubDataUpdater(
                localDownloadFile = PhigrosBot.dataFolderPath.resolve("illustration").toFile(),
                pathOnline = "phigros-res/Illustration",
                branch = "main"
            )
        }
    }
}
