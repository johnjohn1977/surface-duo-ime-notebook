/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuoDeckPresentationPolicyTest {
    @Test
    fun notebookPanelSuppressesTheSystemImeSurface() {
        assertFalse(DuoDeckPresentationPolicy.allowSystemImeView(panelVisible = true))
    }

    @Test
    fun systemImeHostsNotebookSurfaceWhenExternalPanelIsAbsent() {
        assertTrue(DuoDeckPresentationPolicy.allowSystemImeView(panelVisible = false))
    }

    @Test
    fun languageEnginesAreNotPublishedAsSeparateAndroidKeyboards() {
        assertFalse(DuoDeckPresentationPolicy.publishEngineSubtypes())
    }

    @Test
    fun accessibilitySetupIsVisibleOnlyUntilTheServiceConnects() {
        assertTrue(DuoDeckPresentationPolicy.showAccessibilitySetup(serviceConnected = false))
        assertFalse(DuoDeckPresentationPolicy.showAccessibilitySetup(serviceConnected = true))
    }
}
