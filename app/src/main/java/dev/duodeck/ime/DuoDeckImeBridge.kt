/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Narrow process-local bridge to the one component allowed to own InputConnection. */
interface DuoDeckImeSession {
    fun hasActiveInputConnection(): Boolean
    fun onPanelVisibilityChanged()
}

object DuoDeckPanelState {
    private val visible = AtomicBoolean(false)

    fun isVisible(): Boolean = visible.get()

    fun setVisible(value: Boolean) {
        visible.set(value)
    }
}

object DuoDeckImeBridge {
    internal data class Entry(val generation: Long, val session: DuoDeckImeSession)

    private val current = AtomicReference<Entry?>()
    private var nextGeneration = 0L

    @Synchronized
    fun register(session: DuoDeckImeSession): Registration {
        val entry = Entry(++nextGeneration, session)
        current.set(entry)
        return Registration(entry)
    }

    fun current(): DuoDeckImeSession? = current.get()?.session

    class Registration internal constructor(private val entry: Entry) : AutoCloseable {
        override fun close() {
            current.compareAndSet(entry, null)
        }
    }
}
