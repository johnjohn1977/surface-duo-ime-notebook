/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.window.layout.WindowMetricsCalculator;

import dev.duodeck.geometry.FloatPoint;
import dev.duodeck.geometry.PanelGeometry;
import dev.duodeck.geometry.PanelGeometryStore;

public final class DuoDeckAccessibilityService extends AccessibilityService implements GestureDispatcher {
    private static final long TAP_DURATION_MS = 48;
    private static final long LONG_PRESS_DURATION_MS = 650;
    private static final long DRAG_DURATION_MS = 280;
    // Keep a discrete wheel tick shorter than a normal repeated thumb tap.
    // A later physical touch cancels an Accessibility-dispatched gesture, so
    // a long stroke makes rapid wheel taps interfere with one another.
    private static final long SCROLL_DURATION_MS = 80;
    private static final long CURSOR_HIDE_DELAY_MS = 1500;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideCursor = this::removeCursor;
    private WindowManager windowManager;
    private View cursorView;
    private WindowManager.LayoutParams cursorLayout;
    private AccessibilityBridge.Registration registration;
    private PanelGeometry geometry;
    private float pointerX;
    private float pointerY;
    private int cursorSizePx;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (registration != null) {
            registration.close();
        }
        removeCursor();
        windowManager = getSystemService(WindowManager.class);
        cursorSizePx = Math.max(24, Math.round(28 * getResources().getDisplayMetrics().density));
        refreshGeometry();
        FloatPoint center = geometry.workPanelCenter();
        pointerX = center.x;
        pointerY = center.y;
        registration = AccessibilityBridge.register(this);
        // AccessibilityService is not a UI Context on Android 15, so it cannot
        // subscribe to Jetpack WindowManager layout updates. The Activity and
        // IME publish hinge geometry to PanelGeometryStore; refreshGeometry()
        // safely falls back to the display aspect ratio until they do.
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Milestone 1 deliberately does not inspect window content.
    }

    @Override
    public void onInterrupt() {
        // Gesture requests are bounded and do not require retry on interruption.
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(hideCursor);
        disconnectBridge();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Android may unbind an AccessibilityService without destroying its
        // process. Clear the in-process dispatcher immediately so the panel
        // cannot mistake a disabled service for a live one.
        mainHandler.removeCallbacks(hideCursor);
        disconnectBridge();
        return super.onUnbind(intent);
    }

    private void disconnectBridge() {
        if (registration != null) {
            registration.close();
            registration = null;
        }
        removeCursor();
    }

    @Override
    public FloatPoint pointerPosition() {
        return new FloatPoint(pointerX, pointerY);
    }

    @Override
    public void movePointer(float deltaX, float deltaY) {
        refreshGeometryIfDisplayChanged();
        FloatPoint clamped = geometry.clampToWorkPanel(
                pointerX + deltaX,
                pointerY + deltaY,
                cursorSizePx / 2);
        pointerX = clamped.x;
        pointerY = clamped.y;
        revealCursor();
    }

    @Override
    public boolean tap() {
        return dispatchPointGesture(pointerX, pointerY, TAP_DURATION_MS);
    }

    @Override
    public boolean longPress() {
        return dispatchPointGesture(pointerX, pointerY, LONG_PRESS_DURATION_MS);
    }

    @Override
    public boolean drag(FloatPoint from, FloatPoint to) {
        refreshGeometryIfDisplayChanged();
        FloatPoint start = geometry.clampToWorkPanel(from.x, from.y, cursorSizePx / 2);
        FloatPoint end = geometry.clampToWorkPanel(to.x, to.y, cursorSizePx / 2);
        Path path = new Path();
        path.moveTo(start.x, start.y);
        path.lineTo(end.x, end.y);
        revealCursor();
        return dispatchPath(path, DRAG_DURATION_MS);
    }

    @Override
    public boolean scroll(float deltaY) {
        refreshGeometryIfDisplayChanged();
        float maxDistance = Math.max(1f, geometry.workPanel.height() * 0.35f);
        float boundedDelta = Math.max(-maxDistance, Math.min(maxDistance, deltaY));
        FloatPoint end = geometry.clampToWorkPanel(
                pointerX,
                pointerY - boundedDelta,
                cursorSizePx / 2);
        Path path = new Path();
        path.moveTo(pointerX, pointerY);
        path.lineTo(end.x, end.y);
        revealCursor();
        return dispatchPath(path, SCROLL_DURATION_MS);
    }

    private boolean dispatchPointGesture(float x, float y, long durationMs) {
        refreshGeometryIfDisplayChanged();
        FloatPoint point = geometry.clampToWorkPanel(x, y, cursorSizePx / 2);
        Path path = new Path();
        path.moveTo(point.x, point.y);
        revealCursor();
        return dispatchPath(path, durationMs);
    }

    private void revealCursor() {
        mainHandler.removeCallbacks(hideCursor);
        if (cursorView == null) {
            showCursor();
        } else {
            updateCursorPosition();
        }
        mainHandler.postDelayed(hideCursor, CURSOR_HIDE_DELAY_MS);
    }

    private boolean dispatchPath(Path path, long durationMs) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            return false;
        }
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMs))
                .build();
        return dispatchGesture(gesture, null, null);
    }

    private void showCursor() {
        if (windowManager == null || cursorView != null) {
            return;
        }
        cursorView = new CursorView();
        cursorLayout = new WindowManager.LayoutParams(
                cursorSizePx,
                cursorSizePx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        cursorLayout.gravity = Gravity.TOP | Gravity.START;
        updateLayoutCoordinates();
        try {
            windowManager.addView(cursorView, cursorLayout);
        } catch (RuntimeException ignored) {
            cursorView = null;
            cursorLayout = null;
        }
    }

    private void removeCursor() {
        if (windowManager != null && cursorView != null) {
            try {
                windowManager.removeView(cursorView);
            } catch (RuntimeException ignored) {
                // Already detached during service teardown.
            }
        }
        cursorView = null;
        cursorLayout = null;
    }

    private void updateCursorPosition() {
        if (windowManager == null || cursorView == null || cursorLayout == null) {
            return;
        }
        updateLayoutCoordinates();
        try {
            windowManager.updateViewLayout(cursorView, cursorLayout);
        } catch (RuntimeException ignored) {
            removeCursor();
        }
    }

    private void updateLayoutCoordinates() {
        cursorLayout.x = Math.round(pointerX - cursorSizePx / 2f);
        cursorLayout.y = Math.round(pointerY - cursorSizePx / 2f);
    }

    private void refreshGeometryIfDisplayChanged() {
        int[] dimensions = currentDisplayDimensions();
        if (geometry == null
                || geometry.displayWidth != dimensions[0]
                || geometry.displayHeight != dimensions[1]) {
            refreshGeometry();
            FloatPoint clamped = geometry.clampToWorkPanel(pointerX, pointerY, cursorSizePx / 2);
            pointerX = clamped.x;
            pointerY = clamped.y;
        }
    }

    private void refreshGeometry() {
        int[] dimensions = currentDisplayDimensions();
        geometry = new PanelGeometryStore(this).load(dimensions[0], dimensions[1]);
    }

    private int[] currentDisplayDimensions() {
        Rect bounds = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(this)
                .getBounds();
        return new int[]{bounds.width(), bounds.height()};
    }

    private final class CursorView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);

        CursorView() {
            super(DuoDeckAccessibilityService.this);
            fill.setColor(Color.rgb(100, 181, 246));
            outline.setColor(Color.WHITE);
            outline.setStyle(Paint.Style.STROKE);
            outline.setStrokeWidth(Math.max(2f, getResources().getDisplayMetrics().density * 1.5f));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float radius = Math.min(getWidth(), getHeight()) * 0.34f;
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            canvas.drawCircle(centerX, centerY, radius, fill);
            canvas.drawCircle(centerX, centerY, radius, outline);
        }
    }
}
