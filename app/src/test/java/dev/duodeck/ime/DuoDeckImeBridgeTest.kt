/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime

import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class DuoDeckImeBridgeTest {
    @After
    fun clearBridge() {
        DuoDeckImeBridge.register(FakeSession()).close()
        DuoDeckPanelState.setVisible(false)
    }

    @Test
    fun staleRegistrationCannotClearNewSession() {
        val first = FakeSession()
        val second = FakeSession()
        val firstRegistration = DuoDeckImeBridge.register(first)
        val secondRegistration = DuoDeckImeBridge.register(second)

        firstRegistration.close()

        assertSame(second, DuoDeckImeBridge.current())
        secondRegistration.close()
    }

    private class FakeSession : DuoDeckImeSession {
        override fun hasActiveInputConnection() = true
        override fun onPanelVisibilityChanged() = Unit
    }
}
