/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import dev.duodeck.geometry.PanelGeometryObserver
import dev.duodeck.ime.DuoDeckEngineController
import dev.duodeck.ime.DuoDeckImeBridge
import dev.duodeck.ime.DuoDeckPanelState
import dev.duodeck.ui.DuoDeckInputView
import org.fcitx.fcitx5.android.utils.InputMethodUtil

/** Lower split-screen companion that behaves like an external keyboard panel. */
class DuoDeckActivity : ComponentActivity() {
    private lateinit var controller: DuoDeckEngineController
    private lateinit var inputView: DuoDeckInputView
    private lateinit var geometryObserver: PanelGeometryObserver
    private lateinit var inputMethodPickerClient: EditText
    private var panelVisible = false
    private var inputMethodPickerPending = false
    private var inputMethodPickerVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
        )
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )

        controller = DuoDeckEngineController(this) { state ->
            if (::inputView.isInitialized) inputView.updateEngineState(state)
        }
        inputView = DuoDeckInputView(this, controller, ::finishPanel, false)
        setContentView(inputView)
        inputMethodPickerClient = EditText(this).apply {
            alpha = 0f
            isFocusableInTouchMode = true
            showSoftInputOnFocus = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setSingleLine(true)
        }
        addContentView(
            inputMethodPickerClient,
            FrameLayout.LayoutParams(1, 1, Gravity.BOTTOM or Gravity.START)
        )

        geometryObserver = PanelGeometryObserver(this) { inputView.requestLayout() }
        geometryObserver.start()
    }

    override fun onStart() {
        super.onStart()
        setPanelVisible(true)
    }

    override fun onResume() {
        super.onResume()
        inputView.refreshSetupState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::inputView.isInitialized) {
            when {
                inputMethodPickerPending -> showInputMethodPickerFromFocusedClient()
                inputMethodPickerVisible -> restoreNotebookFocusPolicy()
            }
            inputView.refreshSetupState()
        }
    }

    /**
     * The companion normally cannot take focus, which keeps the editor in the upper app active.
     * Android's input-method picker, however, is ignored without a focused calling window. Focus
     * is borrowed only for setup and relinquished as soon as the system picker closes.
     */
    fun requestInputMethodPicker() {
        if (inputMethodPickerPending || inputMethodPickerVisible) return
        inputMethodPickerPending = true
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        inputMethodPickerClient.requestFocus()
        window.decorView.postDelayed({
            if (inputMethodPickerPending && window.decorView.hasWindowFocus()) {
                showInputMethodPickerFromFocusedClient()
            }
        }, PICKER_CLIENT_READY_DELAY_MS)
    }

    private fun showInputMethodPickerFromFocusedClient() {
        if (!inputMethodPickerPending) return
        inputMethodPickerPending = false
        inputMethodPickerClient.requestFocus()
        val inputMethodManager = getSystemService(InputMethodManager::class.java)
        inputMethodManager.restartInput(inputMethodPickerClient)
        inputMethodPickerClient.postDelayed({
            inputMethodPickerVisible = true
            inputMethodManager.showInputMethodPicker()
            window.decorView.postDelayed({
                if (inputMethodPickerVisible && window.decorView.hasWindowFocus()) {
                    // Some Android builds still reject the picker. Leave a public-settings path.
                    restoreNotebookFocusPolicy()
                    InputMethodUtil.startSettingsActivity(this)
                }
            }, PICKER_FOCUS_TIMEOUT_MS)
        }, PICKER_CLIENT_READY_DELAY_MS)
    }

    private fun restoreNotebookFocusPolicy() {
        inputMethodPickerPending = false
        inputMethodPickerVisible = false
        if (::inputMethodPickerClient.isInitialized) {
            inputMethodPickerClient.clearFocus()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    override fun onStop() {
        setPanelVisible(false)
        super.onStop()
    }

    override fun onDestroy() {
        geometryObserver.close()
        controller.close()
        super.onDestroy()
    }

    private fun finishPanel() {
        setPanelVisible(false)
        finishAndRemoveTask()
    }

    private fun setPanelVisible(visible: Boolean) {
        if (panelVisible == visible) return
        panelVisible = visible
        DuoDeckPanelState.setVisible(visible)
        DuoDeckImeBridge.current()?.onPanelVisibilityChanged()
    }

    companion object {
        private const val PICKER_CLIENT_READY_DELAY_MS = 100L
        private const val PICKER_FOCUS_TIMEOUT_MS = 750L
    }
}
