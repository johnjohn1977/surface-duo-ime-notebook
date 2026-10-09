/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.geometry;

import android.content.Context;
import android.graphics.Rect;

import androidx.core.util.Consumer;
import androidx.core.content.ContextCompat;
import androidx.window.java.layout.WindowInfoTrackerCallbackAdapter;
import androidx.window.layout.DisplayFeature;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowInfoTracker;
import androidx.window.layout.WindowLayoutInfo;
import androidx.window.layout.WindowMetricsCalculator;

/** Keeps the shared panel geometry current while its owner is alive. */
public final class PanelGeometryObserver implements AutoCloseable {
    private final Context context;
    private final Consumer<PanelGeometry> callback;
    private final PanelGeometryStore store;
    private final WindowInfoTrackerCallbackAdapter tracker;
    private final Consumer<WindowLayoutInfo> layoutListener = this::onWindowLayoutChanged;
    private boolean started;

    public PanelGeometryObserver(Context context, Consumer<PanelGeometry> callback) {
        this.context = context;
        this.callback = callback;
        store = new PanelGeometryStore(context);
        tracker = new WindowInfoTrackerCallbackAdapter(WindowInfoTracker.getOrCreate(context));
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        tracker.addWindowLayoutInfoListener(
                context, ContextCompat.getMainExecutor(context), layoutListener);
    }

    @Override
    public void close() {
        if (!started) {
            return;
        }
        started = false;
        tracker.removeWindowLayoutInfoListener(layoutListener);
    }

    private void onWindowLayoutChanged(WindowLayoutInfo layoutInfo) {
        Rect display = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(context)
                .getBounds();
        IntRect hinge = separatingHinge(layoutInfo);
        store.save(display.width(), display.height(), hinge);
        callback.accept(PanelGeometry.resolve(display.width(), display.height(), hinge));
    }

    private static IntRect separatingHinge(WindowLayoutInfo layoutInfo) {
        for (DisplayFeature feature : layoutInfo.getDisplayFeatures()) {
            if (feature instanceof FoldingFeature) {
                FoldingFeature fold = (FoldingFeature) feature;
                if (fold.isSeparating()) {
                    Rect bounds = fold.getBounds();
                    return new IntRect(bounds.left, bounds.top, bounds.right, bounds.bottom);
                }
            }
        }
        return null;
    }
}
