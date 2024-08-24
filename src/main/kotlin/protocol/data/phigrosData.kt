package com.github.hatoyuze.protocol.data

import io.ktor.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and

fun ByteBuffer.getVarint(): Int {
    var b: Int
    var num = 0
    var shift = 0

    do {
        b = get().toInt() and 0xFF
        num = num or ((b and 0x7F) shl shift)
        shift += 7
    } while (b and 0x80 != 0)

    return num
}

fun ByteBuffer.getString(): String {
    val length = getVarint()
    if (remaining() < length) {
        throw BufferUnderflowException()
    }
    val result = ByteArray(length)
    get(result)
    return result.decodeToString()
}

fun ByteBuffer.littleEndian(): ByteBuffer {
    order(ByteOrder.LITTLE_ENDIAN)
    return this
}





@Serializable(with = Summary.SummarySerializer::class)
data class Summary(
    val saveVersion: Byte,
    val challengeModeRank: Short,
    val ranking: Float,
    val gameVersion: Byte,
    val avatar: String,
    // We probably shouldn't let it mutable to callers
    val clearCount: MutableList<Short>,
    val fullComboCount: MutableList<Short>,
    val phiCount: MutableList<Short>
) {
    constructor(byteBuffer: ByteBuffer): this(
        byteBuffer.littleEndian().get(),
        byteBuffer.getShort(),
        byteBuffer.getFloat(),
        byteBuffer.get(),
        byteBuffer.getString(),
        mutableListOf(), mutableListOf(), mutableListOf()
    ) {
        // EZ HD IN AT
        for (i in 0 until 4) {
            clearCount.add(byteBuffer.getShort())
            fullComboCount.add(byteBuffer.getShort())
            phiCount.add(byteBuffer.getShort())
        }
    }
    companion object SummarySerializer : KSerializer<Summary>{
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("Summary", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): Summary {
            val original = decoder.decodeString()
            return Summary(ByteBuffer.wrap(original.decodeBase64Bytes()))
        }

        override fun serialize(encoder: Encoder, value: Summary) {
            val byteBuffer = ByteBuffer.allocate(256).littleEndian()

            byteBuffer.put(value.saveVersion)
            byteBuffer.putShort(value.challengeModeRank)
            byteBuffer.putFloat(value.ranking)
            byteBuffer.put(value.gameVersion)

            // put string(the length of this part is unknown)
            if (value.avatar.length > Byte.MAX_VALUE.toInt()) byteBuffer.putShort(value.avatar.length.toShort())
            else byteBuffer.put(value.avatar.length.toByte())
            value.avatar.toByteArray().onEach { byteBuffer.put(it) }


            for (i in 0 until 4) {
                byteBuffer.putShort(value.clearCount[i])
                byteBuffer.putShort(value.fullComboCount[i])
                byteBuffer.putShort(value.phiCount[i])
            }

            byteBuffer.flip() // flip the remaining null bytes
            val byteArray = ByteArray(byteBuffer.remaining())
            byteBuffer.get(byteArray)
            val result = byteArray.encodeBase64()
            encoder.encodeString(result)
        }
    }
}


interface PhigrosFileData {
    object UnknownMember : PhigrosFileData
}
data class SaveOwner(
    val isShowId: Boolean,
    val selfIntro: String,
    val avatar: String,
    val background: String,
): PhigrosFileData {
    constructor(byteBuffer: ByteBuffer) : this(
        byteBuffer.littleEndian().get() and 1 != 0.toByte(),
        byteBuffer.getString(),
        byteBuffer.getString(),
        byteBuffer.getString()
    )
}
@Serializable
data class Score(
    val sid: String,
    val levelIdx: Int,
    val score: Int,
    val achievement: Double,
    val isFullCombo: Boolean
) {
}
@JvmInline
@Serializable
value class SavePlayScore(
    val scores: List<Score>
): PhigrosFileData {
    companion object {
        fun parse(byteBuffer: ByteBuffer): SavePlayScore {
            byteBuffer.littleEndian()
            val songCount = byteBuffer.getVarint()
            val scores = mutableListOf<Score>()
            for (idx in 0 until songCount) {
                val sid = byteBuffer.getString().removeSuffix(".0")
                byteBuffer.getVarint() // unknown field, just skip it
                val levelFlags = byteBuffer.get().toInt()
                val fcFlags = byteBuffer.get().toInt()

                // EZ HD IN AT Legacy?(unknown idx)
                for (level in 0 until 5) {
                    if (levelFlags and (1 shl level) == 0) {
                        continue
                    }
                    val score = byteBuffer.getInt()
                    val acc = byteBuffer.getFloat()
                    val result = Score(
                        sid, level, score, acc.toDouble(),
                        isFullCombo = (fcFlags and (1 shl level) != 0) || (score == 1000000 && acc == 100.0.toFloat())
                    )
                    scores.add(result)
                }
            }
            return SavePlayScore(scores)
        }
    }
}