package com.github.hatoyuze.mirai.command

import com.github.hatoyuze.PhigrosBot
import com.github.hatoyuze.PhigrosBot.logger
import com.github.hatoyuze.image.edit.filterImageColor
import com.github.hatoyuze.image.edit.toRGBString
import com.github.hatoyuze.mirai.data.GlobalAliasLibrary
import com.github.hatoyuze.mirai.data.PhigrosSongData
import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.syncFromEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.awt.Color
import java.io.File
import javax.imageio.ImageIO
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes


/*
* NOTE:
*   Wrap 'sendMessage()' call with 'runCatching' to handle potential 'IllegalStateException'.
*   'sendMessage()' in 'overflow' interface might throw 'IllegalStateException' due to timeout.
*  However, depending on the OneBot implementation, the message could still be sent successfully.
*  Thus, wrapping prevents an 'IllegalStateException' from unintentionally halting the ongoing process.
*/
/**
 * ## “色块游戏”
 *
 * 玩家需要从一张透明的图中，揭开指定颜色的像素，从而猜出其真正对应的歌曲
 *
 * @author HatoYuze
 * */
class PhigrosColorBlockGame private constructor(
    private val contextSubject: Contact,
    private val parentScope: CoroutineScope
) {
    // Some players think this is too easy, should we increase the difficulty ?
    var isAlive = false
        private set
    private var currentUnknownAlphaValue = 0
    private var currentRoundCount = 0
    private var isGameWon = false

    private val guessedList = mutableListOf<Color>()
    private val answerSong by lazy { PhigrosBot.SONGS_DATABASE.random() }
    private val originIllustrationImage = ImageIO.read(answerSong.getIllustration())
    private val tempFileRoot =
        File(System.getProperty("phigros-bot.cache")).resolve("${contextSubject.id}-pcb-games").also { it.mkdirs() }

    suspend fun start(commandContext: CommandContext) {
        kotlin.runCatching {
            commandContext.sendMessage {
                +commandContext.originalMessage.quote()
                +"接下来我们将为您提供一张透明的图片，你可以通过每次的指令揭开该透明图片的部分色块，亦或是猜测图片对应的答案\n"
                +"> 在使用指令时, 请保证您的信息 at 了机器人\n"
                +" - 开 随机\n"
                +" - 开 <red> <green> <blue>\n"
                +" - 猜 <歌名>\n"
                +"您有不超过 5 分钟的时间猜出正确的答案\n"
                +createQuestionImage().toExternalResource().use { contextSubject.uploadImage(it) }
            }
        }

        isAlive = true
        commandContext.checkAnswer<MessageEvent>(answerSong)
    }

    // ONLY call after stopping the main task
    private fun cancelAllProgress() {
        // delete cache
        tempFileRoot.listFiles()?.onEach { it.delete() }
        parentScope.cancel("Canceled all progress by function `cancelAllProgress()`")
        isAlive = false
        runningTasks.remove(contextSubject.id)
    }

    private fun createQuestionImage(): File {
        val bufferedImage = filterImageColor(
            originIllustrationImage,
            guessedList,
        ) { // Change the alpha value of color
            currentUnknownAlphaValue and 0xFF shl 24 or (it and 0x00FFFFFF)
        }
        val outputFile = tempFileRoot.resolve("round$currentRoundCount.png").also { it.createNewFile() }
        ImageIO.write(
            bufferedImage,
            "PNG",
            outputFile
        )
        return outputFile
    }

    private fun uncoverColorBlock(target: Color): Boolean {
        if (target in guessedList) return false
        currentRoundCount++
        currentUnknownAlphaValue += 2
        guessedList.add(target)
        return true
    }

    /**
     * @return Should the game be over
     * */
    private suspend fun handleEventImpl(event: MessageEvent): Boolean {
        fun String.toIntWithAutoRadix(): Int {
            var maxRadix = 10
            if (any { (it.code >= 'a'.code && it.code <= 'f'.code) || (it.code >= 'A'.code && it.code <= 'F'.code)})
                maxRadix = 16
            return toInt(maxRadix)
        }

        suspend fun resolveCommand(
            redOrRandom: String,
            commandGroup: List<String>,
            event: MessageEvent
        ): Color? {
            try {
                return if (redOrRandom.startsWith("random", true) || redOrRandom.startsWith("随机")) {
                    if (Random.nextInt(15) == 0) {
                        // An easy model, random get an ARGB value from origin illustration
                        val randomQueue = mutableSetOf<Color>()
                        for (y in 0 until originIllustrationImage.height) {
                            for (x in 0 until originIllustrationImage.width) {
                                val rgb = originIllustrationImage.getRGB(x, y)
                                randomQueue.add(Color(rgb))
                            }
                        }
                        randomQueue.random()
                    }else Color(Random.nextInt(0xFFFFFFFF.toInt(), Int.MAX_VALUE)) // random a valid ARGB value
                } else {
                    Color(
                        redOrRandom.toIntWithAutoRadix(),
                        commandGroup[COMMAND_UNCOVER_BLUE_INDEX].toIntWithAutoRadix(),
                        commandGroup[COMMAND_UNCOVER_GREEN_INDEX].toIntWithAutoRadix()
                    )
                }
            } catch (e: NumberFormatException) {
                event.quote("指令格式错误\n请确保您输入的是一个有效的数字值\n指令格式: @机器人 开 <red> <green> <blur>\n例如: @机器人 开 31 30 51")
            } catch (e: IllegalArgumentException) {
                event.quote("提供的数量值超过了限定范围！\n请保证您输入的数字值小于等于 255 且 大于 0")
            } catch (e: IndexOutOfBoundsException) {
                event.quote("提供的参数数量过少\n请按照以下指令格式保证参数数量的齐全:\n@机器人 开 <red> <green> <blur>\n例如: @机器人 开 31 30 51")
            }
            return null
        }

        // Check this message is about bot
        val message = event.message
        val atBotInstance = message.findIsInstance<At>()
        if (atBotInstance?.target != event.bot.id)
            return false

        val command = message.filterIsInstance<PlainText>()
            .joinToString(separator = "") { it.content }.trimStart()

        when {
            command.startsWith("猜") -> {
                val content = command.drop("猜".length).trimStart()
                val congruentObject = GlobalAliasLibrary.searchSongWithAlias(content)
                if (answerSong in congruentObject) {
                    isGameWon = true
                    return true
                }
                if (congruentObject.isEmpty())
                    event.quote("没有找到符合输入 $content 的歌曲\n请确认您是否提供的是合法的歌曲？")
                else
                    event.quote("您猜测的答案 ${congruentObject.joinToString(separator = " ", prefix = "", postfix = ""){ it.sid }} 与答案不同!")
            }

            command.startsWith("开") -> {
                if (currentRoundCount >= MAX_ROUNDS) {
                    isGameWon = false
                    return true
                }
                val commandGroup = command.split(' ')
                if (commandGroup.isEmpty()) {
                    event.quote("提供的参数数量过少\n请按照以下指令格式保证参数数量的齐全:\n@机器人 开 <red> <green> <blur>\n或: @机器人 开 随机\n例如: @机器人 开 31 30 51")
                    return false
                }

                val redOrRandom = commandGroup[COMMAND_UNCOVER_RED_INDEX]
                val color = resolveCommand(redOrRandom, commandGroup, event) ?: return false

                if (!uncoverColorBlock(color)) {
                    event.quote("该颜色已经被揭开了哦！\n已经揭开的颜色列表\n${guessedList.joinToString("\n") { it.toRGBString() }}")
                }
                kotlin.runCatching {
                    event.quote(buildMessageChain {
                        +"成功揭开色块 ${color.toRGBString()}\n"
                        +"历史记录: ${guessedList.joinToString { it.toRGBString() }}\n"
                        +createQuestionImage().toExternalResource().use { contextSubject.uploadImage(it) }
                    })
                }

            }
        }
        return false
    }

    private suspend inline fun <reified P : MessageEvent> CommandContext.checkAnswer(
        phigrosSong: PhigrosSongData,
    ) {
        val mapper: suspend (P) -> P? = mapper@{ event ->
            if (event.subject != this.sender.subject) return@mapper null
            if (!handleEventImpl(event)) return@mapper null
            event
        }

        val answer: P? = try {
            parentScope.async {
                withTimeoutOrNull(5.minutes) {
                    GlobalEventChannel.syncFromEvent(EventPriority.NORMAL, mapper)
                }
            }.await()
        } catch (e: Exception) {
            logger.error(e)
            quote("发生了意向不到的错误\n已停止竞猜!\n错误代码: ${e.javaClass.name}")
            cancelAllProgress()
            return
        }

        if (answer == null || !isGameWon) {
            val messageChain = buildMessageChain {
                +originalMessage.quote()
                +"超过了 默认时间/回合限制 自动结束！\n最终的答案为 ${phigrosSong.title} by ${phigrosSong.composer}\n竞猜过程中每回合被揭开的色块: \n${
                    guessedList.joinToString { it.toRGBString() }
                }"
                +phigrosSong.getIllustration().toExternalResource().use {
                    contextSubject.uploadImage(it)
                }
            }
            kotlin.runCatching {
                messageChain.sendTo(contextSubject)
            }
        } else {
            answer.quote(buildMessageChain {
                +"恭喜您猜对了！\n最终的答案为 ${phigrosSong.title} by ${phigrosSong.composer}\n竞猜过程中每回合被揭开的色块: \n${
                    guessedList.joinToString { it.toRGBString() }
                }"
                +phigrosSong.getIllustration().toExternalResource().use {
                    contextSubject.uploadImage(it)
                }
            })
        }
        cancelAllProgress()
    }

    companion object {
        fun new(context: CommandContext, coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)): PhigrosColorBlockGame? {
            val subjectId = context.sender.subject?.id ?: -1
            val isAvailable = runningTasks[subjectId]?.isAlive?.not() ?: true
            if (!isAvailable) return null
            val taskContext = CoroutineScope(
                coroutineScope.coroutineContext + CoroutineName("Phigros-plugin: ColorBlockGame#$subjectId")
            )
            return PhigrosColorBlockGame(
                context.sender.subject ?: error("Commands cannot be executed in the console"),
                taskContext
            ).also { runningTasks[subjectId] = it }
        }

        private val runningTasks = mutableMapOf<Long, PhigrosColorBlockGame>()
        private const val MAX_ROUNDS = 63
        private const val COMMAND_UNCOVER_RED_INDEX = 1
        private const val COMMAND_UNCOVER_GREEN_INDEX = 2
        private const val COMMAND_UNCOVER_BLUE_INDEX = 3

    }
}

/**
 * Send a reply message with message [msg] to current subject
 *
 * Catch any exception that was thrown when sending
 * */
suspend fun MessageEvent.quote(msg: Message) {
    runCatching {
        subject.sendMessage(msg + this.message.quote())
    }
}

/**
 * Send a reply message with plain text [msg] to current subject
 *
 * Catch any exception that was thrown when sending
 * */
suspend fun MessageEvent.quote(msg: String) {
    runCatching {
        subject.sendMessage(PlainText(msg) + this.message.quote())
    }
}