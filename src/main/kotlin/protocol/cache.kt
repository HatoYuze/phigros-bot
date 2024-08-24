package com.github.hatoyuze.protocol

import kotlinx.coroutines.*
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration


class ExpiringCache<T>(
    private val duration: Duration,
    private val onExpire: suspend () -> T,
    private val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : ReadWriteProperty<Any?, T> {
    private var cachedValue: T? = null
    private var expireTime: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)


    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        updateValue()
        return cachedValue ?: throw IllegalStateException("The expiring cache is not being refreshed correctly")
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        updateValue()
    }

    private fun updateValue() {
        val callTime = LocalDateTime.now()
        if (LocalDateTime.now() > expireTime) {
            runBlocking(coroutineContext) {
                refresh()
                expireTime = callTime.plusNanos(duration.inWholeNanoseconds)
            }
        }
    }


    private suspend fun refresh() {
        val newValue = onExpire()
        synchronized(this) {
            cachedValue = newValue
        }
    }
}
fun <T> expiringCacheOf(duration: Duration, onExpire: suspend () -> T) = ExpiringCache(duration,onExpire)