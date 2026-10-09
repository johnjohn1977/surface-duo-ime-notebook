/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.geometry;

public final class PanelGeometry {
    private static final int MIN_POINTER_MARGIN_PX = 8;
    private static final float DUAL_PANEL_MIN_LONG_TO_SHORT_RATIO = 1.45f;

    public final int displayWidth;
    public final int displayHeight;
    public final IntRect workPanel;
    public final IntRect deckPanel;
    public final IntRect hinge;
    public final boolean separating;

    private PanelGeometry(
            int displayWidth,
            int displayHeight,
            IntRect workPanel,
            IntRect deckPanel,
            IntRect hinge,
            boolean separating) {
        this.displayWidth = displayWidth;
        this.displayHeight = displayHeight;
        this.workPanel = workPanel;
        this.deckPanel = deckPanel;
        this.hinge = hinge;
        this.separating = separating;
    }

    public static PanelGeometry resolve(int displayWidth, int displayHeight, IntRect candidateHinge) {
        if (displayWidth <= 0 || displayHeight <= 0) {
            throw new IllegalArgumentException("Display dimensions must be positive");
        }

        IntRect full = new IntRect(0, 0, displayWidth, displayHeight);
        if (isUsableHinge(candidateHinge, displayWidth, displayHeight)) {
            if (candidateHinge.width() >= candidateHinge.height()) {
                IntRect upper = new IntRect(0, 0, displayWidth, candidateHinge.top);
                IntRect lower = new IntRect(0, candidateHinge.bottom, displayWidth, displayHeight);
                if (!upper.isEmpty() && !lower.isEmpty()) {
                    return new PanelGeometry(
                            displayWidth, displayHeight, upper, lower, candidateHinge, true);
                }
            } else {
                IntRect left = new IntRect(0, 0, candidateHinge.left, displayHeight);
                IntRect right = new IntRect(candidateHinge.right, 0, displayWidth, displayHeight);
                if (!left.isEmpty() && !right.isEmpty()) {
                    return new PanelGeometry(
                            displayWidth, displayHeight, left, right, candidateHinge, true);
                }
            }
        }

        float longToShortRatio = Math.max(displayWidth, displayHeight)
                / (float) Math.min(displayWidth, displayHeight);
        if (longToShortRatio < DUAL_PANEL_MIN_LONG_TO_SHORT_RATIO) {
            return new PanelGeometry(displayWidth, displayHeight, full, full, null, false);
        }

        if (displayHeight >= displayWidth) {
            int seam = displayHeight / 2;
            return new PanelGeometry(
                    displayWidth,
                    displayHeight,
                    new IntRect(0, 0, displayWidth, seam),
                    new IntRect(0, seam, displayWidth, displayHeight),
                    new IntRect(0, seam, displayWidth, seam),
                    false);
        }

        int seam = displayWidth / 2;
        return new PanelGeometry(
                displayWidth,
                displayHeight,
                new IntRect(0, 0, seam, displayHeight),
                new IntRect(seam, 0, displayWidth, displayHeight),
                new IntRect(seam, 0, seam, displayHeight),
                false);
    }

    private static boolean isUsableHinge(IntRect hinge, int displayWidth, int displayHeight) {
        if (hinge == null || hinge.isEmpty() || !hinge.isInside(displayWidth, displayHeight)) {
            return false;
        }
        boolean horizontal = hinge.width() >= displayWidth / 2 && hinge.height() < displayHeight / 2;
        boolean vertical = hinge.height() >= displayHeight / 2 && hinge.width() < displayWidth / 2;
        return horizontal || vertical;
    }

    public FloatPoint clampToWorkPanel(float x, float y, int requestedMarginPx) {
        int margin = Math.max(MIN_POINTER_MARGIN_PX, requestedMarginPx);
        float minX = Math.min(workPanel.right, workPanel.left + margin);
        float maxX = Math.max(workPanel.left, workPanel.right - margin);
        float minY = Math.min(workPanel.bottom, workPanel.top + margin);
        float maxY = Math.max(workPanel.top, workPanel.bottom - margin);
        return new FloatPoint(clamp(x, minX, maxX), clamp(y, minY, maxY));
    }

    public FloatPoint workPanelCenter() {
        return new FloatPoint(
                workPanel.left + workPanel.width() / 2f,
                workPanel.top + workPanel.height() / 2f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
