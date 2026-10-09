/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.geometry;

import java.util.Objects;

public final class IntRect {
    public final int left;
    public final int top;
    public final int right;
    public final int bottom;

    public IntRect(int left, int top, int right, int bottom) {
        if (right < left || bottom < top) {
            throw new IllegalArgumentException("Invalid rectangle");
        }
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public int width() {
        return right - left;
    }

    public int height() {
        return bottom - top;
    }

    public boolean isEmpty() {
        return width() == 0 || height() == 0;
    }

    public boolean isInside(int width, int height) {
        return left >= 0 && top >= 0 && right <= width && bottom <= height;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IntRect)) {
            return false;
        }
        IntRect that = (IntRect) other;
        return left == that.left && top == that.top && right == that.right && bottom == that.bottom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, top, right, bottom);
    }

    @Override
    public String toString() {
        return "IntRect(" + left + "," + top + "-" + right + "," + bottom + ")";
    }
}
