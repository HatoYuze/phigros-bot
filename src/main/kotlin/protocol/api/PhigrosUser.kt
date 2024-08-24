package com.github.hatoyuze.protocol.api

import com.github.hatoyuze.mirai.data.GlobalUserData
import com.github.hatoyuze.protocol.data.ResolvedPhigrosGameSaveResp
import com.github.hatoyuze.protocol.expiringCacheOf
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes

class PhigrosUser(
    private val sessionToken: String
) {
    val userName by expiringCacheOf(10.minutes) {
        PhigrosApiImpl.userData(sessionToken).nickname
    }

    val userInternalId by lazy {
        runBlocking {
            PhigrosApiImpl.userData(sessionToken).objectId
        }
    }
    private val userProfiles by expiringCacheOf(1.minutes) {
        PhigrosApiImpl.entryData(sessionToken)
    }
    val userSummary by expiringCacheOf(1.minutes) {
        userProfiles.summary.also {
            GlobalUserData.ranking[sessionToken.hashCode()] = it.ranking
        }
    }
    val userInfo by expiringCacheOf(10.minutes) {
        userProfiles.saveOwner
    }
    val playScore by expiringCacheOf(1.minutes) {
        userProfiles.savePlayScore.also {
            GlobalUserData.score[sessionToken.hashCode()] = it
        }
    }
    val lastUpdateTime: LocalDateTime by expiringCacheOf(1.minutes) {
        ZonedDateTime.parse(userProfiles.updatedAt, ResolvedPhigrosGameSaveResp.TIME_STRING_FORMATTER)
            .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }
}