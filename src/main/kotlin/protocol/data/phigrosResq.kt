package com.github.hatoyuze.protocol.data

import com.github.hatoyuze.protocol.data.PhigrosSaveZipFileEntry.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Serializable
data class UserInfoResp(
    val nickname: String,
    val updatedAt: String,
    val createdAt: String,
    val objectId: String
)

@Serializable
data class PlaySaveResp(
    val results: List<PhigrosSaveResultResp>
)

@Serializable
data class PhigrosSaveResultResp(
    val createdAt: String,
    val updatedAt: String,
    val summary: Summary,
    val gameFile: PhigrosSaveResultGameFileResp,
)
@Serializable
data class PhigrosSaveResultGameFileResp(
    val createdAt: String,
    val url: String,
    val metaData: PhigrosSaveResultFileMetaDataResp,
)

@Serializable
data class PhigrosSaveResultFileMetaDataResp(
    // NOTE: 仅在用户的第一个存档对象(index[0])存在
    @SerialName("_checksum")
    val checksum: String? = null,
    val size: Int
)

data class ResolvedPhigrosGameSaveResp(
    val summary: Summary,
    val saveOwner: SaveOwner,
    val savePlayScore: SavePlayScore,
    val updatedAt: String
) {
    companion object {
        val TIME_STRING_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
    }
}

enum class PhigrosSaveZipFileEntry(val filename: String) {
    SETTINGS("settings"), // unknown member
    USER_INFO("user"),
    GAME_PROGRESS("gameProgress"), // unknown member
    GAME_RECORD("gameRecord"),
    GAME_KEY("gameKey"); // unknown member
    companion object {
        val SUPPORT_LIST = listOf(USER_INFO, GAME_RECORD)
        fun String.nameOfEntry(): PhigrosSaveZipFileEntry {
            return PhigrosSaveZipFileEntry.entries.find { it.filename == this } ?: throw IllegalArgumentException("$this is not a valid value ")
        }
    }
}

sealed interface PhigrosZipDataImpl<T: PhigrosFileData> {
    val key: PhigrosSaveZipFileEntry
    val data: ByteArray
    fun invoke(): T

    class GameRecordImpl(override val data: ByteArray) : PhigrosZipDataImpl<SavePlayScore> {
        override val key: PhigrosSaveZipFileEntry = GAME_RECORD
        override fun invoke(): SavePlayScore {
            return SavePlayScore.parse(ByteBuffer.wrap(data))
        }

    }
    class UserInfoImpl(override val data: ByteArray) : PhigrosZipDataImpl<SaveOwner> {
        override val key: PhigrosSaveZipFileEntry = USER_INFO
        override fun invoke(): SaveOwner {
            return SaveOwner(ByteBuffer.wrap(data))
        }
    }
    class UnknownMember(override val key: PhigrosSaveZipFileEntry, override val data: ByteArray) : PhigrosZipDataImpl<PhigrosFileData.UnknownMember> {
        override fun invoke(): PhigrosFileData.UnknownMember {
            error("Unknown member cannot invoke()")
        }
    }
    companion object {
        fun PhigrosSaveZipFileEntry.wrap(data: ByteArray) = when(this) {
            SETTINGS, GAME_PROGRESS, GAME_KEY -> UnknownMember(this, data)
            USER_INFO -> UserInfoImpl(data)
            GAME_RECORD -> GameRecordImpl(data)
        }

        // The implementation here is just TOO BAD, I think we need to optimize it as soon as possible
        @Suppress("UNCHECKED_CAST")
        operator fun <T: PhigrosZipDataImpl<*>> List<PhigrosZipDataImpl<*>>.get(key: PhigrosSaveZipFileEntry)
            = find { it.key == key } as? T? ?: throw IllegalArgumentException("Zip file does not contain $key")
    }
}
