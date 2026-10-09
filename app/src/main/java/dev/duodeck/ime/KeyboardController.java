/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime;

/** Sends keyboard actions to the currently active editor without owning UI focus. */
public interface KeyboardController {
    void handleKey(NotebookLayoutSpec.Key key);

    void selectCandidate(int index);

    void pageCandidates(int delta);

    void toggleLanguage();

    boolean isModifierActive(NotebookLayoutSpec.Modifier modifier);

    boolean hasActiveInputConnection();
}
