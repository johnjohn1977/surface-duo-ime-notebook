/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.geometry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PanelGeometryTest {
    @Test
    public void hingeCreatesUpperWorkAndLowerDeck() {
        PanelGeometry geometry = PanelGeometry.resolve(
                1800, 2784, new IntRect(0, 1350, 1800, 1434));

        assertTrue(geometry.separating);
        assertEquals(new IntRect(0, 0, 1800, 1350), geometry.workPanel);
        assertEquals(new IntRect(0, 1434, 1800, 2784), geometry.deckPanel);
    }

    @Test
    public void absentHingeUsesDuoAspectRatioFallback() {
        PanelGeometry portrait = PanelGeometry.resolve(1800, 2784, null);
        PanelGeometry ordinary = PanelGeometry.resolve(1350, 1800, null);

        assertFalse(portrait.separating);
        assertEquals(new IntRect(0, 0, 1800, 1392), portrait.workPanel);
        assertEquals(new IntRect(0, 1392, 1800, 2784), portrait.deckPanel);
        assertEquals(new IntRect(0, 0, 1350, 1800), ordinary.deckPanel);
    }

    @Test
    public void pointerIsClampedInsideWorkPanelMargin() {
        PanelGeometry geometry = PanelGeometry.resolve(
                1800, 2784, new IntRect(0, 1350, 1800, 1434));

        FloatPoint point = geometry.clampToWorkPanel(-500, 9000, 20);

        assertEquals(20f, point.x, 0.01f);
        assertEquals(1330f, point.y, 0.01f);
    }
}
