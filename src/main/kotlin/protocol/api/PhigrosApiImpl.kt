package com.github.hatoyuze.protocol.api

import com.github.hatoyuze.protocol.data.*
 import com.github.hatoyuze.protocol.data.PhigrosZipDataImpl.Companion.wrap
 import com.github.hatoyuze.protocol.data.PhigrosZipDataImpl.Companion.get
 import com.github.hatoyuze.protocol.data.PhigrosSaveZipFileEntry.Companion.nameOfEntry
import com.github.hatoyuze.protocol.net.Network
import com.github.hatoyuze.protocol.net.PhigrosFeature

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val net = Network("https://rak3ffdi.cloud.tds1.tapapis.cn/1.1") {
    install(PhigrosFeature)
}

internal object PhigrosApiImpl {
    suspend fun userData(sessionToken: String) = net.tapCall<UserInfoResp>("/users/me", sessionToken)

    suspend fun playSaveImpl(sessionToken: String) = net.tapCall<PlaySaveResp>("/classes/_GameSave", sessionToken)


    suspend fun entryData(sessionToken: String): ResolvedPhigrosGameSaveResp {
        val resp0 =  playSaveImpl(sessionToken).results.first()
        val gameFile = resp0.gameFile
        val data = resolveSave(
            gameFile.url,
            PhigrosSaveZipFileEntry.SUPPORT_LIST,
            gameFile.metaData.size,
            gameFile.metaData.checksum
        )
        val info: PhigrosZipDataImpl.UserInfoImpl = data[PhigrosSaveZipFileEntry.USER_INFO]
        val score: PhigrosZipDataImpl.GameRecordImpl = data[PhigrosSaveZipFileEntry.GAME_RECORD]
        return ResolvedPhigrosGameSaveResp(
            resp0.summary,
            info.invoke(),
            score.invoke(),
            resp0.updatedAt
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun resolveSave(
        saveUrl: String,
        requestedEntry: List<PhigrosSaveZipFileEntry>? = null,
        verifySize: Int? = null,
        verifyMd5: String? = null
    ): List<PhigrosZipDataImpl<*>> {
        val result = mutableListOf<PhigrosZipDataImpl<*>>()
        val downlandBody = HttpClient(CIO).get(saveUrl).body<ByteArray>()
        // verify data to avoid data is incorrect
        if ((verifySize != null && verifySize != downlandBody.size) || (verifyMd5 != null && downlandBody.md5() != verifyMd5))
            throw IllegalStateException("The downloaded content does not conform to the metadata")

        // parse data
        ZipInputStream(
            downlandBody.inputStream()
        ).use { stream ->
            var entry: ZipEntry? = null
            while (stream.nextEntry?.also { entry = it } != null) {
                // skip the element which isn't in requested list
                val fileOfKey = entry?.name?.nameOfEntry() ?: error("Zip file has not initialized")
                if (requestedEntry != null && fileOfKey !in requestedEntry) {
                    continue
                }
                val buffer = ByteArray(1536) // eqs 1.5kb
                val decryptData = ByteArrayOutputStream().use { outputStream ->
                    var length: Int
                    // NOTE: We MUST skip the first byte of the entry bytes
                    //   To implement this, we need 'isDropped' to point which part needs to skip
                    var isDropped = false
                    while (stream.read(buffer).also { length = it } != -1) {
                        if (!isDropped) {
                            outputStream.write(buffer, 1, length - 1)
                            isDropped = true
                        } else outputStream.write(buffer, 0, length) // That means the first byte was skipped before
                    }
                    aesDecrypt(outputStream.toByteArray())
                }

                result.add(
                    fileOfKey.wrap(decryptData)
                )

                entry = null
                stream.closeEntry()
            }
        }
        return result
    }
}



private val key = "6Jaa0qVAJZuXkZCLiOa/Ax5tIZVu+taKUN1V1nqwkks=".decodeBase64Bytes()
private val iv = "Kk/wisgNYwcAV8WVGMgyUw==".decodeBase64Bytes()

// Should we add the algorithms for encrypting? We do not need it so far.
private fun aesDecrypt(data: ByteArray): ByteArray {
    fun ByteArray.pad(blockSize: Int = 16): ByteArray {
        if (size % 16 == 0) {
            return this
        }
        val paddedBytes = ByteArray((size + blockSize - 1) / blockSize * blockSize)
        paddedBytes.fill(0)
        System.arraycopy(this, 0, paddedBytes, 0, size)
        return paddedBytes
    }

    val keySpec = SecretKeySpec(key.pad(), "AES")
    val ivSpec = IvParameterSpec(iv)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    return cipher.doFinal(data.pad())
}

@OptIn(ExperimentalStdlibApi::class)
fun ByteArray.md5(format: HexFormat = HexFormat.Default) = MessageDigest.getInstance("MD5").digest(this).toHexString(format)

private val json = Json {
    ignoreUnknownKeys = true
}

private suspend inline fun <reified T> Network.tapCall(
    action: String,
    sessionToken: String,
): T {
    val response = execute(action) {
        method = HttpMethod.Get
        header("X-LC-Session", sessionToken)
    }
    if (response.status == HttpStatusCode.BadRequest) {
        throw IllegalArgumentException("Error 400 bad request\n" +
            " With headers: ${response.request.headers}")
    }
    if (response.status != HttpStatusCode.OK) {
        throw IllegalStateException("An error happened because of wrong response status: ${response.status}\n With headers: ${response.request.headers}")
    }
    val body = response.bodyAsText()
    if (body.isEmpty() || body == "null") {
        throw IllegalStateException("Response returns nothing")
    }
    return json.decodeFromString<T>(body)
}