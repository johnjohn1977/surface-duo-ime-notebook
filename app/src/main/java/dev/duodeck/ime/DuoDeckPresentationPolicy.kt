/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime

/**
 * Product boundary for Notebook Mode.
 *
 * Fcitx is always the input engine behind one Notebook surface. The system
 * IME hosts that surface when the external Notebook panel is absent.
 */
object DuoDeckPresentationPolicy {
    fun allowSystemImeView(panelVisible: Boolean): Boolean = !panelVisible

    fun publishEngineSubtypes(): Boolean = false

    fun showAccessibilitySetup(serviceConnected: Boolean): Boolean = !serviceConnected
}
