/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime;

import java.util.Collections;
import java.util.List;

/** Immutable presentation state emitted by the Fcitx engine controller. */
public final class DuoDeckEngineState {
    public final String preedit;
    public final List<String> candidates;
    public final String inputMethodLabel;
    public final boolean chineseActive;
    public final boolean chewingAvailable;

    public DuoDeckEngineState(
            String preedit,
            List<String> candidates,
            String inputMethodLabel,
            boolean chineseActive,
            boolean chewingAvailable) {
        this.preedit = preedit;
        this.candidates = Collections.unmodifiableList(candidates);
        this.inputMethodLabel = inputMethodLabel;
        this.chineseActive = chineseActive;
        this.chewingAvailable = chewingAvailable;
    }

    public static DuoDeckEngineState initial() {
        return new DuoDeckEngineState("", Collections.emptyList(), "EN", false, false);
    }
}
