/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ui;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import dev.duodeck.DuoDeckActivity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.duodeck.accessibility.AccessibilityBridge;
import dev.duodeck.accessibility.GestureDispatcher;
import dev.duodeck.geometry.FloatPoint;
import dev.duodeck.geometry.PanelGeometry;
import dev.duodeck.geometry.PanelGeometryStore;
import dev.duodeck.ime.DuoDeckEngineState;
import dev.duodeck.ime.DuoDeckPresentationPolicy;
import dev.duodeck.ime.KeyboardController;
import dev.duodeck.ime.NotebookLayoutSpec;
import dev.duodeck.ime.ZhuyinKeyLabels;
import org.fcitx.fcitx5.android.R;
import org.fcitx.fcitx5.android.utils.InputMethodUtil;

@SuppressLint({"ViewConstructor", "ClickableViewAccessibility"})
public final class DuoDeckInputView extends LinearLayout {
    private static final float KEYBOARD_WEIGHT = 0.516f;
    private static final float TOUCHPAD_WEIGHT = 0.404f;
    private static final float CONTROLS_WEIGHT = 0.080f;
    private static final int KEY_GUTTER_DP = 3;
    private static final int SCROLL_WHEEL_POSITIONS = 6;
    private static final int SCROLL_WHEEL_FINE_HOLD_MS = 400;
    private static final int SCROLL_WHEEL_TIMEOUT_MS = 500;
    private static final int SCROLL_WHEEL_WIDTH_DP = 64;
    private static final int SCROLL_STEP_DP = 48;

    private final Context context;
    private final KeyboardController keyboardController;
    private final Runnable hideAction;
    private final boolean constrainToDeckPanel;
    private final PanelGeometryStore geometryStore;
    private final Map<NotebookLayoutSpec.Modifier, List<Button>> modifierButtons =
            new EnumMap<>(NotebookLayoutSpec.Modifier.class);
    private final Map<NotebookLayoutSpec.Key, Button> textButtons = new LinkedHashMap<>();
    private final List<Button> scrollWheelButtons = new ArrayList<>();
    private final TouchpadSurface touchpad;
    private final TextView touchpadLabel;
    private final Button dragButton;
    private final Button languageButton;
    private final LinearLayout candidateStrip;
    private final LinearLayout accessibilitySetup;
    private final LinearLayout imeSetup;
    private final TextView preeditView;
    private final LinearLayout candidatesRow;
    private boolean chineseActive;
    private int scrollWheelVisualGeneration;
    private int scrollWheelGestureStart = -1;
    private int scrollWheelGestureCurrent = -1;
    private boolean scrollWheelGestureMoved;
    private boolean scrollWheelFineMode;
    private AccessibilityBridge.Registration accessibilityStatusRegistration;

    public DuoDeckInputView(
            Context context,
            KeyboardController keyboardController,
            Runnable hideAction,
            boolean constrainToDeckPanel) {
        super(context);
        this.context = context;
        this.keyboardController = keyboardController;
        this.hideAction = hideAction;
        this.constrainToDeckPanel = constrainToDeckPanel;
        geometryStore = new PanelGeometryStore(context);
        setOrientation(VERTICAL);
        setBackgroundColor(getColor(R.color.deck_background));
        setOnApplyWindowInsetsListener((view, insets) -> {
            int navigationBarBottom = navigationBarBottomInset(insets);
            if (getPaddingBottom() != navigationBarBottom) {
                // Android's IME switcher shares the transparent navigation
                // bar. Keep Notebook controls above that system-owned area so
                // it never overlaps the Duo Deck EN/Chinese key.
                setPadding(
                        getPaddingLeft(),
                        getPaddingTop(),
                        getPaddingRight(),
                        navigationBarBottom);
            }
            return insets;
        });

        LinearLayout keyboard = buildKeyboard();
        addView(keyboard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, KEYBOARD_WEIGHT));

        FrameLayout touchpadFrame = new FrameLayout(context);
        touchpad = new TouchpadSurface();
        touchpadFrame.addView(touchpad, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        touchpadLabel = new TextView(context);
        touchpadLabel.setText(R.string.duo_deck_touchpad_hint);
        touchpadLabel.setTextColor(getColor(R.color.deck_muted));
        touchpadLabel.setTextSize(12);
        touchpadLabel.setGravity(Gravity.CENTER);
        touchpadLabel.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        touchpadFrame.addView(touchpadLabel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        accessibilitySetup = new LinearLayout(context);
        accessibilitySetup.setOrientation(VERTICAL);
        accessibilitySetup.setGravity(Gravity.CENTER);
        accessibilitySetup.setPadding(dp(18), dp(10), dp(18), dp(10));
        TextView accessibilityExplanation = new TextView(context);
        accessibilityExplanation.setText(R.string.duo_deck_accessibility_setup_explanation);
        accessibilityExplanation.setTextColor(getColor(R.color.deck_text));
        accessibilityExplanation.setTextSize(13);
        accessibilityExplanation.setGravity(Gravity.CENTER);
        accessibilitySetup.addView(accessibilityExplanation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Button accessibilityButton = controlButton(
                getResources().getString(R.string.duo_deck_enable_touchpad),
                ignored -> openAccessibilitySettings());
        accessibilityButton.setContentDescription(getResources().getString(
                R.string.duo_deck_enable_touchpad_description));
        LinearLayout.LayoutParams accessibilityButtonParams = new LinearLayout.LayoutParams(
                dp(220), dp(46));
        accessibilityButtonParams.topMargin = dp(8);
        accessibilitySetup.addView(accessibilityButton, accessibilityButtonParams);
        FrameLayout.LayoutParams accessibilitySetupParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        accessibilitySetupParams.setMarginEnd(dp(SCROLL_WHEEL_WIDTH_DP + 2));
        touchpadFrame.addView(accessibilitySetup, accessibilitySetupParams);

        imeSetup = new LinearLayout(context);
        imeSetup.setOrientation(VERTICAL);
        imeSetup.setGravity(Gravity.CENTER);
        imeSetup.setPadding(dp(18), dp(10), dp(18), dp(10));
        TextView imeExplanation = new TextView(context);
        imeExplanation.setText(R.string.duo_deck_ime_setup_explanation);
        imeExplanation.setTextColor(getColor(R.color.deck_text));
        imeExplanation.setTextSize(13);
        imeExplanation.setGravity(Gravity.CENTER);
        imeSetup.addView(imeExplanation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Button imeButton = controlButton(
                getResources().getString(R.string.duo_deck_select_input_method),
                ignored -> openInputMethodPicker());
        imeButton.setContentDescription(getResources().getString(
                R.string.duo_deck_select_input_method_description));
        LinearLayout.LayoutParams imeButtonParams = new LinearLayout.LayoutParams(
                dp(240), dp(46));
        imeButtonParams.topMargin = dp(8);
        imeSetup.addView(imeButton, imeButtonParams);
        FrameLayout.LayoutParams imeSetupParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        imeSetupParams.setMarginEnd(dp(SCROLL_WHEEL_WIDTH_DP + 2));
        touchpadFrame.addView(imeSetup, imeSetupParams);

        candidateStrip = new LinearLayout(context);
        candidateStrip.setOrientation(HORIZONTAL);
        candidateStrip.setGravity(Gravity.CENTER_VERTICAL);
        candidateStrip.setPadding(dp(3), dp(2), dp(3), dp(2));
        candidateStrip.setBackgroundColor(getColor(R.color.deck_background));

        preeditView = new TextView(context);
        preeditView.setTextColor(getColor(R.color.deck_accent));
        preeditView.setTextSize(15);
        preeditView.setGravity(Gravity.CENTER_VERTICAL);
        preeditView.setSingleLine(true);
        candidateStrip.addView(preeditView, new LinearLayout.LayoutParams(dp(92),
                ViewGroup.LayoutParams.MATCH_PARENT));

        Button previous = controlButton("‹", view -> keyboardController.pageCandidates(-1));
        previous.setContentDescription(getResources().getString(
                R.string.duo_deck_previous_candidates));
        candidateStrip.addView(previous, new LinearLayout.LayoutParams(
                dp(38), ViewGroup.LayoutParams.MATCH_PARENT));

        HorizontalScrollView candidateScroller = new HorizontalScrollView(context);
        candidateScroller.setHorizontalScrollBarEnabled(false);
        candidateScroller.setFillViewport(true);
        candidatesRow = new LinearLayout(context);
        candidatesRow.setOrientation(HORIZONTAL);
        candidateScroller.addView(candidatesRow, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        candidateStrip.addView(candidateScroller, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Button next = controlButton("›", view -> keyboardController.pageCandidates(1));
        next.setContentDescription(getResources().getString(R.string.duo_deck_next_candidates));
        candidateStrip.addView(next, new LinearLayout.LayoutParams(
                dp(38), ViewGroup.LayoutParams.MATCH_PARENT));
        candidateStrip.setVisibility(GONE);

        FrameLayout.LayoutParams candidateParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44), Gravity.TOP);
        candidateParams.setMarginEnd(dp(SCROLL_WHEEL_WIDTH_DP + 2));
        touchpadFrame.addView(candidateStrip, candidateParams);

        FrameLayout scrollWheelHost = new FrameLayout(context);
        LinearLayout scrollWheelView = new LinearLayout(context);
        scrollWheelView.setOrientation(VERTICAL);
        scrollWheelView.setPadding(dp(2), dp(2), dp(2), dp(2));
        scrollWheelView.setBackgroundColor(ColorCompat.withAlpha(
                getColor(R.color.deck_background), 230));
        for (int position = 0; position < SCROLL_WHEEL_POSITIONS; position++) {
            Button wheelButton = scrollWheelButton(position);
            scrollWheelButtons.add(wheelButton);
            scrollWheelView.addView(wheelButton, scrollWheelButtonParams());
        }
        scrollWheelHost.addView(scrollWheelView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        View scrollWheelTouchTarget = new View(context);
        scrollWheelTouchTarget.setContentDescription(getResources().getString(
                R.string.duo_deck_scroll_wheel_gesture));
        scrollWheelTouchTarget.setOnTouchListener(this::handleScrollWheelTouch);
        scrollWheelHost.addView(scrollWheelTouchTarget, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams scrollWheelParams = new FrameLayout.LayoutParams(
                dp(SCROLL_WHEEL_WIDTH_DP),
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END);
        touchpadFrame.addView(scrollWheelHost, scrollWheelParams);
        addView(touchpadFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, TOUCHPAD_WEIGHT));

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(HORIZONTAL);
        controls.setPadding(dp(3), dp(2), dp(3), dp(3));
        dragButton = controlButton(getResources().getString(R.string.duo_deck_left_drag), view -> {
            touchpad.toggleDragMode();
            refreshDragButton();
        });
        controls.addView(dragButton, weighted());
        controls.addView(controlButton(getResources().getString(R.string.duo_deck_right_hold), view -> {
            GestureDispatcher dispatcher = AccessibilityBridge.current();
            if (dispatcher == null || !dispatcher.longPress()) {
                showAccessibilityUnavailable();
            }
        }), weighted());
        languageButton = controlButton(
                getResources().getString(R.string.duo_deck_switch_to_chinese),
                view -> keyboardController.toggleLanguage());
        LinearLayout.LayoutParams languageParams = new LinearLayout.LayoutParams(
                dp(SCROLL_WHEEL_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT);
        languageParams.setMargins(dp(2), 0, dp(2), 0);
        controls.addView(languageButton, languageParams);
        addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, CONTROLS_WEIGHT));

        refreshModifierState();
        refreshDragButton();
        refreshSetupState();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (accessibilityStatusRegistration == null) {
            accessibilityStatusRegistration = AccessibilityBridge.observeConnection(
                    connected -> post(this::refreshSetupState));
        }
        requestApplyInsets();
        refreshSetupState();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (accessibilityStatusRegistration != null) {
            accessibilityStatusRegistration.close();
            accessibilityStatusRegistration = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        WindowManager manager = context.getSystemService(WindowManager.class);
        if (constrainToDeckPanel
                && manager != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Maximum metrics stay tied to the full spanned display even after
            // the IME window itself has been constrained to one panel.
            Rect bounds = manager.getMaximumWindowMetrics().getBounds();
            PanelGeometry geometry = geometryStore.load(bounds.width(), bounds.height());
            int safePanelHeight = Math.min(
                    geometry.deckPanel.height(),
                    Math.round(bounds.height() * 0.52f));
            int mode = View.MeasureSpec.getMode(heightMeasureSpec);
            int availableHeight = View.MeasureSpec.getSize(heightMeasureSpec);
            int targetHeight = mode == View.MeasureSpec.UNSPECIFIED
                    ? safePanelHeight
                    : Math.min(safePanelHeight, availableHeight);
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    targetHeight, View.MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void refreshModifierState() {
        for (Map.Entry<NotebookLayoutSpec.Modifier, List<Button>> entry : modifierButtons.entrySet()) {
            boolean active = keyboardController.isModifierActive(entry.getKey());
            for (Button button : entry.getValue()) {
                applyButtonBackground(button, active);
            }
        }
        boolean shift = keyboardController.isModifierActive(NotebookLayoutSpec.Modifier.SHIFT);
        boolean caps = keyboardController.isModifierActive(NotebookLayoutSpec.Modifier.CAPS);
        for (Map.Entry<NotebookLayoutSpec.Key, Button> entry : textButtons.entrySet()) {
            applyKeyLegend(entry.getValue(), entry.getKey(), shift, caps);
        }
    }

    public void updateEngineState(DuoDeckEngineState state) {
        preeditView.setText(state.preedit);
        languageButton.setText(state.chineseActive
                ? R.string.duo_deck_switch_to_english
                : R.string.duo_deck_switch_to_chinese);
        languageButton.setContentDescription(getResources().getString(
                state.chineseActive
                        ? R.string.duo_deck_switch_to_english_description
                        : R.string.duo_deck_switch_to_chinese_description));
        applyButtonBackground(languageButton, state.chineseActive);
        if (chineseActive != state.chineseActive) {
            chineseActive = state.chineseActive;
            refreshModifierState();
        }

        candidatesRow.removeAllViews();
        for (int index = 0; index < state.candidates.size(); index++) {
            final int candidateIndex = index;
            Button candidate = controlButton(
                    state.candidates.get(index),
                    view -> keyboardController.selectCandidate(candidateIndex));
            candidate.setTextSize(15);
            candidate.setMinWidth(dp(64));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            params.setMargins(dp(2), 0, dp(2), 0);
            candidatesRow.addView(candidate, params);
        }
        candidateStrip.setVisibility(
                state.preedit.isEmpty() && state.candidates.isEmpty() ? GONE : VISIBLE);
    }

    public void refreshSetupState() {
        boolean imeSelected = InputMethodUtil.INSTANCE.isSelected();
        boolean serviceConnected = AccessibilityBridge.current() != null;
        boolean showAccessibilitySetup = imeSelected
                && DuoDeckPresentationPolicy.INSTANCE.showAccessibilitySetup(serviceConnected);
        imeSetup.setVisibility(imeSelected ? GONE : VISIBLE);
        accessibilitySetup.setVisibility(showAccessibilitySetup ? VISIBLE : GONE);
        touchpadLabel.setVisibility(
                imeSelected && !showAccessibilitySetup ? VISIBLE : GONE);
    }

    private LinearLayout buildKeyboard() {
        LinearLayout keyboard = new LinearLayout(context);
        keyboard.setOrientation(VERTICAL);
        keyboard.setPadding(
                dp(KEY_GUTTER_DP),
                dp(KEY_GUTTER_DP),
                dp(KEY_GUTTER_DP),
                dp(KEY_GUTTER_DP));

        List<NotebookLayoutSpec.Key> keys = NotebookLayoutSpec.keys();
        for (int rowIndex = 0; rowIndex < NotebookLayoutSpec.ROWS; rowIndex++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            keyboard.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            for (NotebookLayoutSpec.Key key : keys) {
                if (key.row != rowIndex) {
                    continue;
                }
                Button button = buildKeyButton(key);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, key.span);
                // Margins are outside the Button's hit rectangle. Adjacent
                // keys therefore have a real, non-clickable 6dp gutter rather
                // than only a visual separator.
                int margin = dp(KEY_GUTTER_DP);
                params.setMargins(margin, margin, margin, margin);
                row.addView(button, params);
            }
        }
        return keyboard;
    }

    private Button buildKeyButton(NotebookLayoutSpec.Key key) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(key.label);
        button.setTextColor(getColor(R.color.deck_text));
        button.setTextSize(key.row == 0 ? 10 : 13);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        applyButtonBackground(button, false);
        button.setOnClickListener(view -> {
            if (key.kind == NotebookLayoutSpec.Kind.HIDE) {
                hideAction.run();
            } else {
                keyboardController.handleKey(key);
            }
            refreshModifierState();
        });

        if (key.kind == NotebookLayoutSpec.Kind.MODIFIER) {
            modifierButtons
                    .computeIfAbsent(key.modifier, ignored -> new ArrayList<>())
                    .add(button);
        } else if (key.kind == NotebookLayoutSpec.Kind.TEXT) {
            textButtons.put(key, button);
        }
        return button;
    }

    private void applyKeyLegend(
            Button button,
            NotebookLayoutSpec.Key key,
            boolean shiftActive,
            boolean capsActive) {
        String zhuyin = chineseActive ? ZhuyinKeyLabels.forKey(key) : null;
        if (zhuyin == null) {
            String label = key.displayLabel(shiftActive, capsActive);
            button.setText(label);
            button.setTextSize(key.row == 0 ? 10 : 13);
            button.setContentDescription(label);
            button.setMaxLines(1);
            button.setIncludeFontPadding(true);
            button.setLineSpacing(0f, 1f);
            return;
        }

        // Keep the phonetic symbol dominant for touch typing while retaining
        // the underlying QWERTY key as a smaller landmark for shortcuts.
        String qwerty = key.label;
        SpannableString legend = new SpannableString(zhuyin + "\n" + qwerty);
        int qwertyStart = zhuyin.length() + 1;
        legend.setSpan(
                new RelativeSizeSpan(0.66f),
                qwertyStart,
                legend.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        legend.setSpan(
                new ForegroundColorSpan(getColor(R.color.deck_muted)),
                qwertyStart,
                legend.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        button.setText(legend);
        button.setTextSize(15);
        button.setContentDescription(zhuyin + "，" + qwerty);
        button.setMaxLines(2);
        button.setIncludeFontPadding(false);
        button.setLineSpacing(0f, 0.88f);
    }

    private Button controlButton(String label, OnClickListener listener) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(getColor(R.color.deck_text));
        button.setTextSize(12);
        button.setPadding(0, 0, 0, 0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        applyButtonBackground(button, false);
        button.setOnClickListener(listener);
        return button;
    }

    private Button scrollWheelButton(int position) {
        String label;
        if (position == 0) {
            label = "↑";
        } else if (position == SCROLL_WHEEL_POSITIONS - 1) {
            label = "↓";
        } else {
            label = "│";
        }
        Button button = controlButton(label, null);
        button.setTextSize(15);
        button.setContentDescription(getResources().getString(
                R.string.duo_deck_scroll_wheel_position, position + 1));
        button.setClickable(false);
        button.setFocusable(false);
        button.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        return button;
    }

    private LinearLayout.LayoutParams scrollWheelButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        params.setMargins(0, dp(1), 0, dp(1));
        return params;
    }

    private boolean handleScrollWheelTouch(View view, MotionEvent event) {
        int position = ScrollWheelGesture.positionForY(
                event.getY(), view.getHeight(), SCROLL_WHEEL_POSITIONS);
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN
                && scrollWheelGestureStart < 0) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                scrollWheelGestureStart = position;
                scrollWheelGestureCurrent = position;
                scrollWheelGestureMoved = false;
                scrollWheelFineMode = false;
                highlightScrollWheelPosition(position);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (position != scrollWheelGestureCurrent) {
                    captureScrollWheelFirstMove(event);
                    scrollWheelGestureCurrent = position;
                    highlightScrollWheelPosition(position);
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    if (dispatchFineScroll()) {
                        return true;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (position != scrollWheelGestureCurrent) {
                    captureScrollWheelFirstMove(event);
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
                scrollWheelGestureCurrent = position;
                highlightScrollWheelPosition(position);
                if (dispatchFineScroll()) {
                    return true;
                }
                scheduleScrollWheelClear();
                int steps = ScrollWheelGesture.stepsForRelease(
                        scrollWheelGestureStart,
                        scrollWheelGestureCurrent,
                        SCROLL_WHEEL_POSITIONS,
                        scrollWheelFineMode);
                resetScrollWheelGesture();
                if (steps != 0) {
                    performDiscreteScroll(steps);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                resetScrollWheelGesture();
                clearScrollWheelPosition();
                return true;
            default:
                return true;
        }
    }

    private void captureScrollWheelFirstMove(MotionEvent event) {
        if (scrollWheelGestureMoved) {
            return;
        }
        scrollWheelGestureMoved = true;
        scrollWheelFineMode = ScrollWheelGesture.isFineScrollFirstMove(
                event.getDownTime(),
                event.getEventTime(),
                SCROLL_WHEEL_FINE_HOLD_MS);
    }

    private boolean dispatchFineScroll() {
        if (!scrollWheelFineMode) {
            return false;
        }
        int steps = ScrollWheelGesture.stepsForRelease(
                scrollWheelGestureStart,
                scrollWheelGestureCurrent,
                SCROLL_WHEEL_POSITIONS,
                true);
        scheduleScrollWheelClear();
        resetScrollWheelGesture();
        if (steps != 0) {
            performDiscreteScroll(steps);
        }
        return true;
    }

    private void resetScrollWheelGesture() {
        scrollWheelGestureStart = -1;
        scrollWheelGestureCurrent = -1;
        scrollWheelGestureMoved = false;
        scrollWheelFineMode = false;
    }

    private void highlightScrollWheelPosition(int position) {
        ++scrollWheelVisualGeneration;
        for (int index = 0; index < scrollWheelButtons.size(); index++) {
            applyButtonBackground(scrollWheelButtons.get(index), index == position);
        }
    }

    private void scheduleScrollWheelClear() {
        int generation = scrollWheelVisualGeneration;
        postDelayed(() -> {
            if (scrollWheelVisualGeneration != generation) {
                return;
            }
            clearScrollWheelPosition();
        }, SCROLL_WHEEL_TIMEOUT_MS);
    }

    private void clearScrollWheelPosition() {
        ++scrollWheelVisualGeneration;
        for (Button button : scrollWheelButtons) {
            applyButtonBackground(button, false);
        }
    }

    private void performDiscreteScroll(int steps) {
        GestureDispatcher dispatcher = AccessibilityBridge.current();
        if (dispatcher == null || !dispatcher.scroll(dp(SCROLL_STEP_DP) * (float) steps)) {
            showAccessibilityUnavailable();
        }
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private void refreshDragButton() {
        boolean active = touchpad != null && touchpad.dragMode;
        dragButton.setText(active ? R.string.duo_deck_drag_armed : R.string.duo_deck_left_drag);
        applyButtonBackground(dragButton, active);
    }

    private void applyButtonBackground(Button button, boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getColor(active ? R.color.deck_key_active : R.color.deck_key));
        drawable.setCornerRadius(dp(7));
        drawable.setStroke(dp(1), active ? getColor(R.color.deck_accent) : ColorCompat.withAlpha(
                getColor(R.color.deck_muted), 80));
        button.setBackground(drawable);
    }

    private void showAccessibilityUnavailable() {
        refreshSetupState();
        Toast.makeText(
                context,
                getResources().getString(R.string.duo_deck_accessibility_unavailable),
                Toast.LENGTH_SHORT).show();
    }

    private void openInputMethodPicker() {
        // Android owns the final choice; the app only opens the public picker.
        if (context instanceof DuoDeckActivity) {
            ((DuoDeckActivity) context).requestInputMethodPicker();
        } else {
            InputMethodUtil.INSTANCE.showPicker();
        }
    }

    private void openAccessibilitySettings() {
        try {
            // Android deliberately keeps the final service selection and consent
            // in Settings. There is no public API for an app to enable itself.
            Intent settings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            // Keep Settings out of the panel's task so Back returns to the
            // Notebook surface instead of leaving Settings stacked above it.
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settings);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(
                    context,
                    getResources().getString(R.string.duo_deck_accessibility_settings_unavailable),
                    Toast.LENGTH_LONG).show();
        }
    }

    private int getColor(int colorResource) {
        return context.getColor(colorResource);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int navigationBarBottomInset(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
        }
        return insets.getSystemWindowInsetBottom();
    }

    private static final class ColorCompat {
        static int withAlpha(int color, int alpha) {
            return (color & 0x00ffffff) | ((alpha & 0xff) << 24);
        }
    }

    private final class TouchpadSurface extends View {
        private static final long LONG_PRESS_MS = 500;

        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float lastX;
        private float lastY;
        private float travel;
        private long downAt;
        private FloatPoint dragStart;
        private boolean dragMode;
        private boolean secondaryClickDispatched;

        TouchpadSurface() {
            super(context);
            setBackgroundColor(getColor(R.color.deck_surface));
            border.setColor(ColorCompat.withAlpha(getColor(R.color.deck_muted), 100));
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(dp(1));
        }

        void toggleDragMode() {
            dragMode = !dragMode;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = dp(5);
            canvas.drawRoundRect(
                    inset,
                    inset,
                    getWidth() - inset,
                    getHeight() - inset,
                    dp(12),
                    dp(12),
                    border);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            GestureDispatcher dispatcher = AccessibilityBridge.current();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    secondaryClickDispatched = false;
                    downAt = SystemClock.uptimeMillis();
                    lastX = event.getX();
                    lastY = event.getY();
                    travel = 0f;
                    dragStart = dispatcher == null ? null : dispatcher.pointerPosition();
                    return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == 2 && !secondaryClickDispatched) {
                        secondaryClickDispatched = true;
                        // Post after the physical event returns. The injected
                        // long press intentionally cancels the remaining
                        // two-finger stream and acts as Android's context click.
                        post(() -> {
                            GestureDispatcher currentDispatcher = AccessibilityBridge.current();
                            if (currentDispatcher == null || !currentDispatcher.longPress()) {
                                showAccessibilityUnavailable();
                            }
                        });
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (dispatcher == null) {
                        return true;
                    }
                    if (secondaryClickDispatched || event.getPointerCount() >= 2) {
                        return true;
                    }
                    float x = event.getX();
                    float y = event.getY();
                    float deltaX = x - lastX;
                    float deltaY = y - lastY;
                    travel += Math.abs(deltaX) + Math.abs(deltaY);
                    dispatcher.movePointer(deltaX * 1.65f, deltaY * 1.65f);
                    lastX = x;
                    lastY = y;
                    return true;
                case MotionEvent.ACTION_POINTER_UP:
                    return true;
                case MotionEvent.ACTION_UP:
                    if (secondaryClickDispatched) {
                        return true;
                    }
                    if (dispatcher == null) {
                        showAccessibilityUnavailable();
                        return true;
                    }
                    long held = SystemClock.uptimeMillis() - downAt;
                    if (dragMode && dragStart != null && travel >= dp(8)) {
                        dispatcher.drag(dragStart, dispatcher.pointerPosition());
                        dragMode = false;
                        refreshDragButton();
                    } else if (travel < dp(10)) {
                        if (held >= LONG_PRESS_MS) {
                            dispatcher.longPress();
                        } else {
                            performClick();
                        }
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
                default:
                    return super.onTouchEvent(event);
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            GestureDispatcher dispatcher = AccessibilityBridge.current();
            if (dispatcher == null || !dispatcher.tap()) {
                showAccessibilityUnavailable();
            }
            return true;
        }
    }
}
