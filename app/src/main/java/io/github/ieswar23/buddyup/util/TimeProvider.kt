package io.github.ieswar23.buddyup.util

import javax.inject.Inject

/** Abstraction over the system clock so time-dependent logic stays testable. */
fun interface TimeProvider {
    fun now(): Long
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
