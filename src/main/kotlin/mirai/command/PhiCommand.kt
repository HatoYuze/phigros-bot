package com.github.hatoyuze.mirai.command

import com.github.hatoyuze.PhigrosBot
import com.github.hatoyuze.mirai.data.GlobalAliasLibrary
import com.github.hatoyuze.mirai.data.GlobalUserData
import com.github.hatoyuze.mirai.data.PhigrosLevel
import com.github.hatoyuze.mirai.game.PhigrosBest19Result
import com.github.hatoyuze.mirai.game.PhigrosBest19Result.Companion.stringWithPoint
import com.github.hatoyuze.mirai.game.PhigrosTool
import com.github.hatoyuze.protocol.data.Score
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.RootPermission
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File
import com.github.hatoyuze.mirai.data.GlobalUserData as bindings

object PhiCommand : CompositeCommand(
    PhigrosBot, "phi", "臀",
    description = "A command set for phigros",

    ) {
    @SubCommand("bind", "绑定")
    @Description("绑定 sessionToken")
    suspend fun bind(commandContext: CommandContext, sessionToken: String): Unit = commandContext.run {
        bindings[senderId] = sessionToken
        val user = bindings[senderId] ?: run {
            quote("貌似没有成功绑定 sessionToken？ 请撤回原始信息稍后再试")
            return
        }
        try {
            val name = user.userName
            quote("成功绑定用户 $name\n请尽快撤回您的 sessionToken 以避免泄漏！")
        } catch (e: IllegalArgumentException) {
            quote(
                """
                您貌似提供了一个错误的 sessionToken! 请确认是否正确!
                  - Android 系统下的获取方式
                   访问 /.../Android/data/com.PigeonGames.Phigros/file/.userdata 
                   并截取文件末段 "sessionToken": "此为您的sessionToken"
            """.trimIndent()
            )
        }
    }

    @SubCommand("addAlias", "aa")
    suspend fun addAlias(commandContext: CommandContext, song: String, newAlias: String): Unit =
        commandContext.run cmd@{
            if (!GlobalAliasLibrary.isNormalUserAllowed && !sender.hasPermission(RootPermission)) {
                quote("当前规定了无法添加别名！\n您无法通过指令添加别名，请联系机器人所有者！")
                return@cmd
            }
            val searchSongWithAlias = GlobalAliasLibrary.searchSongWithAlias(song)
            if (searchSongWithAlias.isEmpty()) {
                quote("没有找到名称为 $song 的可用歌曲哦！\n请尝试输入全称")
                return@cmd
            }
            if (searchSongWithAlias.size > 1) {
                quote("找到多个解析项，无法确定所要增加别名的对象\n${searchSongWithAlias.joinToString { "${it.title}(${it.sid})" }}\n请尝试使用 sid 添加别名\n例如: Another Me(kalpa) 则可使用 'AnotherMe.DAAN' 代替")
                return@cmd
            }
            val obj = searchSongWithAlias.first()
            obj.addAlias(newAlias) ?: run {
                quote("意料之外的错误，暂时无法添加别名!")
                return@cmd
            }
            quote(
                "成功为歌曲 #${obj.title} (sid=${obj.sid}) 添加别名\n该歌曲目前拥有以下别名:\n${
                    obj.queryAliases().joinToString(limit = 15) { it }
                }"
            )
        }

    @SubCommand("alias")
    suspend fun alias(commandContext: CommandContext, alias: String): Unit = commandContext.run {
        val searchSongWithAlias = GlobalAliasLibrary.searchSongWithAlias(alias)
        if (searchSongWithAlias.isEmpty()) {
            quote("没有找到名称为 $alias 的歌曲！")
            return
        }
        if (searchSongWithAlias.size > 1) {
            quote(
                """
                |找到了多个结果
                | ${searchSongWithAlias.joinToString { "【${it.title}】 by ${it.composer}\nsid=${it.sid}" }}
            """.trimMargin()
            )
            return
        }
        val resultInfo = buildString {
            val result = searchSongWithAlias.first()
            appendLine("【${result.title}】")
            appendLine(" 艺术家: ${result.composer} 曲绘作者: ${result.illustrator}")
            appendLine("-------")
            result.charts.forEach {
                appendLine("【${it.level}】 定数: ${it.rating} 谱师：${it.charter}")
            }
            append("本曲对应 sid 为 ${result.sid}")
        }
        quote(resultInfo + image(searchSongWithAlias.first().getIllustration()))
    }

    @SubCommand("b19")
    suspend fun b19(commandContext: CommandContext): Unit = commandContext.run {
        fun List<PhigrosBest19Result.BestItem>.infoString(): String {
            return buildString {
                this@infoString.forEachIndexed { index, bestItem ->
                    appendLine("#${index.plus(1)} $bestItem")
                }
                if (lastIndex != -1)
                    deleteAt(lastIndex)
            }
        }

        val phigrosUser = bindings[senderId] ?: run {
            quote("您没有绑定 sessionToken 哦！\n请使用指令绑定 ->/phi bind 您的sessionToken")
            return
        }
        val best19Result = PhigrosTool.getB19(phigrosUser)
        sendForwardMessage(
            "Player: ${best19Result.userNick}\nRks: ${
                best19Result.rks.toString().take(5)
            }\n Rks = (best19 + phi1) / 20",
            best19Result.phi1?.let { bestItem ->
                "Phi1: $bestItem\n"
            } ?: "Phi1: 暂无记录\n"
                .plus(best19Result.best19.infoString()),
            "位于 b19 外的歌曲:\n${best19Result.bestExtended3.infoString()}\n位于 phi1 外的歌曲:\n${best19Result.phiExtended3.infoString()}"
        )
    }

    @SubCommand
    suspend fun info(commandContext: CommandContext, title: String) = commandContext.run {
        fun Score.comboStatus() = when {
            score < 70_0000 -> "False"
            score == 100_0000 -> "All perfect"
            isFullCombo -> "Full combo"
            else -> "Clear"
        }

        val phigrosUser = bindings[senderId] ?: run {
            quote("您没有绑定 sessionToken 哦！\n请使用指令绑定 ->/phi bind 您的sessionToken")
            return
        }
        val songData = GlobalAliasLibrary.searchSongWithAlias(title).firstOrNull() ?: kotlin.run {
            quote("没有找到歌名为 $title 的歌曲哦！ 请尝试输入全名！")
            return
        }

        fun Score.info() =
            """
                | 【${PhigrosLevel.id(levelIdx)}】 难度: ${songData.charts[levelIdx].rating} [${comboStatus()}]
                |   ${PhigrosBest19Result.getRank(score)} ${
                score.toString().padStart(7, '0')
            } (acc: ${this.achievement.stringWithPoint(2)}%)
            """.trimMargin()

        val scores = phigrosUser.playScore.scores.filter {
            it.sid == songData.sid
        }
        sendMessage {
            +"""
            |【${songData.title}】 by ${songData.composer}
            |  sid: ${songData.sid} 
            |游玩记录：
            | ${scores.joinToString("\n") { it.info() }.ifEmpty { "暂无游玩记录！" }}""".trimMargin()
            +image(songData.getIllustration())
        }
    }

    @OptIn(ConsoleExperimentalApi::class)
    @SubCommand("ra")
    suspend fun ra(commandContext: CommandContext, @Name("定数") constant: Double, acc: Double) = commandContext.run {
        quote(
            "若一首定数 $constant 的歌曲达成率为 $acc\n则对应 ra 为 ${
                PhigrosTool.getRankingScore(constant, acc).stringWithPoint(4)
            }"
        )
    }

    @SubCommand
    @Description("获取用户排行榜")
    suspend fun rank(commandContext: CommandContext) = commandContext.run {
        val rank = GlobalUserData.users
            .mapKeys { it.value.userInternalId }
            .mapKeys { GlobalUserData.ranking[it.key.hashCode()] ?: it.value.userSummary.ranking }
            .entries
            .sortedBy { -it.key }
            .take(10)

        commandContext.sender.sendMessage(
            buildMessageChain {
                rank.forEachIndexed { index, entry ->
                    +"#${index.plus(1)} ${entry.value.userName} rks: ${entry.key}\n"
                }
                +"所得数据来源自内部缓存，可能存在 误差/延迟"
                +originalMessage.quote()
            }
        )
    }
}

val CommandContext.senderId get() = sender.user?.id ?: -1
suspend fun CommandContext.quote(msg: String) {
    this.sender.sendMessage(
        PlainText(msg).plus(originalMessage.quote())
    )
}

suspend inline fun CommandContext.sendMessage(block: MessageChainBuilder.() -> Unit) {
    val messageChain = buildMessageChain(block)
    if (sender.subject == null) {
        sender.sendMessage(messageChain.content)
    } else {
        sender.subject!!.sendMessage(messageChain)
    }
}

suspend fun CommandContext.image(file: File): Message {
    return file.toExternalResource().use {
        sender.subject?.uploadImage(it) ?: PlainText("Image { ${file.name} }")
    }
}

suspend fun CommandContext.sendForwardMessage(vararg message: String) {
    val subject = sender.subject ?: kotlin.run {
        println(message.joinToString("\n") { it })
        return
    }
    subject.sendMessage(
        buildForwardMessage(subject) {
            for (msg in message) {
                sender.bot!! says PlainText(msg)
            }
        }
    )

}