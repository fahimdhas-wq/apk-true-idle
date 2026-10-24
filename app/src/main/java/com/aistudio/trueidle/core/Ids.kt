package com.aistudio.trueidle.core

import java.util.concurrent.atomic.AtomicLong

class Ids(initialNextId: Long = 1L) {
    private val counter = AtomicLong(initialNextId.coerceAtLeast(1L))

    fun next(): Long = counter.getAndIncrement()

    fun current(): Long = counter.get()

    fun reset(nextId: Long) {
        counter.set(nextId.coerceAtLeast(1L))
    }
}
