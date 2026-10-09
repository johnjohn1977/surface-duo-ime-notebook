/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ui;

/** Pure geometry and release semantics for the thumb-driven scroll strip. */
final class ScrollWheelGesture {
    private ScrollWheelGesture() {}

    static int positionForY(float y, int height, int positionCount) {
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        if (positionCount < 2) {
            throw new IllegalArgumentException("positionCount must be at least two");
        }
        float clampedY = Math.max(0f, Math.min((float) height, y));
        int position = (int) (clampedY * positionCount / height);
        return Math.min(positionCount - 1, position);
    }

    static boolean isFineScrollFirstMove(long downTimeMs, long eventTimeMs, long holdMs) {
        if (holdMs < 0) {
            throw new IllegalArgumentException("holdMs must not be negative");
        }
        return eventTimeMs >= downTimeMs && eventTimeMs - downTimeMs >= holdMs;
    }

    static int stepsForRelease(
            int startPosition,
            int endPosition,
            int positionCount,
            boolean fineScroll) {
        requirePosition(startPosition, positionCount);
        requirePosition(endPosition, positionCount);
        if (startPosition != endPosition) {
            if (fineScroll) {
                return Integer.signum(endPosition - startPosition);
            }
            return endPosition - startPosition;
        }
        if (startPosition == 0) {
            return -1;
        }
        if (startPosition == positionCount - 1) {
            return 1;
        }
        return 0;
    }

    private static void requirePosition(int position, int positionCount) {
        if (positionCount < 2) {
            throw new IllegalArgumentException("positionCount must be at least two");
        }
        if (position < 0 || position >= positionCount) {
            throw new IllegalArgumentException("position is outside the scroll strip");
        }
    }
}
