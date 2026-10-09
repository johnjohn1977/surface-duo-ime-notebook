/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.geometry;

import android.content.Context;
import android.content.SharedPreferences;

public final class PanelGeometryStore {
    private static final String PREFS = "panel_geometry";
    private static final String WIDTH = "display_width";
    private static final String HEIGHT = "display_height";
    private static final String LEFT = "hinge_left";
    private static final String TOP = "hinge_top";
    private static final String RIGHT = "hinge_right";
    private static final String BOTTOM = "hinge_bottom";
    private static final String HAS_HINGE = "has_hinge";

    private final SharedPreferences preferences;

    public PanelGeometryStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(int displayWidth, int displayHeight, IntRect hinge) {
        SharedPreferences.Editor editor = preferences.edit()
                .putInt(WIDTH, displayWidth)
                .putInt(HEIGHT, displayHeight)
                .putBoolean(HAS_HINGE, hinge != null);
        if (hinge != null) {
            editor.putInt(LEFT, hinge.left)
                    .putInt(TOP, hinge.top)
                    .putInt(RIGHT, hinge.right)
                    .putInt(BOTTOM, hinge.bottom);
        }
        editor.apply();
    }

    public PanelGeometry load(int displayWidth, int displayHeight) {
        boolean dimensionsMatch = preferences.getInt(WIDTH, -1) == displayWidth
                && preferences.getInt(HEIGHT, -1) == displayHeight;
        IntRect hinge = null;
        if (dimensionsMatch && preferences.getBoolean(HAS_HINGE, false)) {
            hinge = new IntRect(
                    preferences.getInt(LEFT, 0),
                    preferences.getInt(TOP, 0),
                    preferences.getInt(RIGHT, 0),
                    preferences.getInt(BOTTOM, 0));
        }
        return PanelGeometry.resolve(displayWidth, displayHeight, hinge);
    }
}
