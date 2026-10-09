/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.accessibility;

import dev.duodeck.geometry.FloatPoint;

public interface GestureDispatcher {
    FloatPoint pointerPosition();

    void movePointer(float deltaX, float deltaY);

    boolean tap();

    boolean longPress();

    boolean drag(FloatPoint from, FloatPoint to);

    boolean scroll(float deltaY);
}
