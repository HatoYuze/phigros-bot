package com.github.hatoyuze.mirai.data

import com.github.hatoyuze.protocol.api.PhigrosUser
import com.github.hatoyuze.protocol.data.SavePlayScore
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object GlobalUserData : AutoSavePluginData("globalPlayScore"){
    // int=userId.hashCode()
    val score: MutableMap<Int, SavePlayScore> by value(mutableMapOf())
    // int=userId.hashCode()
    val ranking: MutableMap<Int,Float> by value(mutableMapOf())
    // session token bindings
    val qqUserBinding: MutableMap<Long, String> by value(mutableMapOf())

    val users = mutableMapOf<Long, PhigrosUser>()

    operator fun get(id: Long): PhigrosUser? {
        return users[id] ?: qqUserBinding[id]?.let { token ->
            PhigrosUser(token).also { set(id, token) }
        }
    }
    operator fun set(id: Long, value: String) {
        users[id] = PhigrosUser(value)
        qqUserBinding[id] = value
    }
}