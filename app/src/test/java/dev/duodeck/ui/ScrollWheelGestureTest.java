/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class ScrollWheelGestureTest {
    @Test
    public void yCoordinatesMapToSixBoundedPositions() {
        assertEquals(0, ScrollWheelGesture.positionForY(-20f, 600, 6));
        assertEquals(0, ScrollWheelGesture.positionForY(0f, 600, 6));
        assertEquals(2, ScrollWheelGesture.positionForY(299f, 600, 6));
        assertEquals(3, ScrollWheelGesture.positionForY(300f, 600, 6));
        assertEquals(5, ScrollWheelGesture.positionForY(599f, 600, 6));
        assertEquals(5, ScrollWheelGesture.positionForY(800f, 600, 6));
    }

    @Test
    public void releaseUsesNetDistanceAndDirection() {
        assertEquals(4, ScrollWheelGesture.stepsForRelease(1, 5, 6, false));
        assertEquals(-3, ScrollWheelGesture.stepsForRelease(4, 1, 6, false));
    }

    @Test
    public void fineScrollClampsMovementToOneStep() {
        assertEquals(1, ScrollWheelGesture.stepsForRelease(1, 5, 6, true));
        assertEquals(-1, ScrollWheelGesture.stepsForRelease(4, 1, 6, true));
        assertEquals(1, ScrollWheelGesture.stepsForRelease(2, 3, 6, true));
        assertEquals(-1, ScrollWheelGesture.stepsForRelease(3, 2, 6, true));
    }

    @Test
    public void fineScrollStartsOnlyWhenFirstMoveFollowsHoldThreshold() {
        assertEquals(false, ScrollWheelGesture.isFineScrollFirstMove(1_000, 1_399, 400));
        assertEquals(true, ScrollWheelGesture.isFineScrollFirstMove(1_000, 1_400, 400));
        assertEquals(false, ScrollWheelGesture.isFineScrollFirstMove(1_400, 1_000, 400));
        assertThrows(IllegalArgumentException.class,
                () -> ScrollWheelGesture.isFineScrollFirstMove(1_000, 1_400, -1));
    }

    @Test
    public void endCapsRemainUsefulAsSingleStepButtons() {
        assertEquals(-1, ScrollWheelGesture.stepsForRelease(0, 0, 6, false));
        assertEquals(1, ScrollWheelGesture.stepsForRelease(5, 5, 6, false));
        assertEquals(0, ScrollWheelGesture.stepsForRelease(3, 3, 6, false));
        assertEquals(-1, ScrollWheelGesture.stepsForRelease(0, 0, 6, true));
        assertEquals(1, ScrollWheelGesture.stepsForRelease(5, 5, 6, true));
        assertEquals(0, ScrollWheelGesture.stepsForRelease(3, 3, 6, true));
    }

    @Test
    public void rejectsInvalidGeometryAndPositions() {
        assertThrows(IllegalArgumentException.class,
                () -> ScrollWheelGesture.positionForY(0f, 0, 6));
        assertThrows(IllegalArgumentException.class,
                () -> ScrollWheelGesture.stepsForRelease(-1, 1, 6, false));
        assertThrows(IllegalArgumentException.class,
                () -> ScrollWheelGesture.stepsForRelease(1, 6, 6, false));
    }
}
