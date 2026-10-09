/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime

import android.content.Context
import android.view.KeyEvent
import android.widget.Toast
import dev.duodeck.ime.NotebookLayoutSpec.Kind
import dev.duodeck.ime.NotebookLayoutSpec.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxAPI
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.KeyState
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.FcitxDaemon
import java.util.EnumSet
import java.util.function.Consumer

/**
 * Serializes Duo Deck key actions through the shared Fcitx engine. The Android
 * IME service remains the sole owner of InputConnection and receives the
 * resulting commit/preedit events through the existing Fcitx event path.
 */
class DuoDeckEngineController(
    context: Context,
    private val clientName: String = PANEL_CLIENT_NAME,
    private val stateConsumer: Consumer<DuoDeckEngineState>
) : KeyboardController, AutoCloseable {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connection: FcitxConnection = FcitxDaemon.connect(clientName)
    private val operations = Channel<suspend FcitxAPI.() -> Unit>(Channel.UNLIMITED)
    private val modifiers = EnumSet.noneOf(Modifier::class.java)

    private var clientPreedit = ""
    private var panelPreedit = ""
    private var candidates = emptyList<String>()
    private var currentIme = ENGLISH_IME
    private var currentImeLabel = "EN"
    private var chewingAvailable = false
    private var closed = false

    init {
        scope.launch {
            for (operation in operations) {
                connection.runOnReady(operation)
            }
        }
        scope.launch {
            connection.runImmediately { eventFlow }.collect(::handleEvent)
        }
        enqueue { refreshEngineState(this) }
    }

    override fun handleKey(key: NotebookLayoutSpec.Key) {
        if (!hasActiveInputConnection()) {
            toast(R.string.duo_deck_no_active_input_target)
            return
        }
        when (key.kind) {
            Kind.MODIFIER -> toggleModifier(key.modifier)
            Kind.HIDE -> Unit
            Kind.TEXT -> {
                val states = currentKeyStates()
                val value = key.committedText(false, false)
                if (!value.isNullOrEmpty()) {
                    enqueue {
                        if (value.length == 1) {
                            sendKey(value[0], states, up = false)
                            sendKey(value[0], states, up = true)
                        } else {
                            sendKey(value, states, up = false)
                            sendKey(value, states, up = true)
                        }
                    }
                }
                clearOneShotModifiers()
            }
            Kind.SPECIAL -> {
                val symbol = specialKeyName(key.keyCode)
                if (symbol != null) {
                    val states = currentKeyStates()
                    enqueue {
                        sendKey(symbol, states, up = false)
                        sendKey(symbol, states, up = true)
                    }
                }
                clearOneShotModifiers()
            }
        }
        publishState()
    }

    override fun selectCandidate(index: Int) {
        if (index < 0 || index >= candidates.size) return
        enqueue { select(index) }
    }

    override fun pageCandidates(delta: Int) {
        if (delta == 0) return
        enqueue { offsetCandidatePage(delta.coerceIn(-1, 1)) }
    }

    override fun toggleLanguage() {
        enqueue {
            val available = availableIme()
            chewingAvailable = available.any { it.uniqueName == CHEWING_IME }
            if (!chewingAvailable) {
                scope.launch { toast(R.string.duo_deck_chewing_missing) }
                publishState()
                return@enqueue
            }

            val target = if (currentIme == CHEWING_IME) ENGLISH_IME else CHEWING_IME
            val enabled = enabledIme().map { it.uniqueName }.toMutableList()
            if (target !in enabled) {
                enabled += target
                setEnabledIme(enabled.distinct().toTypedArray())
            }
            activateIme(target)
            refreshEngineState(this)
        }
    }

    override fun isModifierActive(modifier: Modifier): Boolean = modifiers.contains(modifier)

    override fun hasActiveInputConnection(): Boolean =
        DuoDeckImeBridge.current()?.hasActiveInputConnection() == true

    override fun close() {
        if (closed) return
        closed = true
        operations.close()
        scope.cancel()
        FcitxDaemon.disconnect(clientName)
    }

    private fun handleEvent(event: FcitxEvent<*>) {
        when (event) {
            is FcitxEvent.CandidateListEvent -> {
                candidates = event.data.candidates.map { candidate ->
                    buildString {
                        if (candidate.label.isNotBlank()) append(candidate.label).append(' ')
                        append(candidate.textWithComment())
                    }
                }
                publishState()
            }
            is FcitxEvent.ClientPreeditEvent -> {
                clientPreedit = event.data.toString()
                publishState()
            }
            is FcitxEvent.InputPanelEvent -> {
                panelPreedit = event.data.preedit.toString()
                publishState()
            }
            is FcitxEvent.IMChangeEvent -> {
                currentIme = event.data.uniqueName
                currentImeLabel = event.data.nativeName.ifBlank { event.data.displayName }
                publishState()
            }
            is FcitxEvent.ReadyEvent -> enqueue { refreshEngineState(this) }
            else -> Unit
        }
    }

    private suspend fun refreshEngineState(api: FcitxAPI) = with(api) {
        val available = availableIme()
        chewingAvailable = available.any { it.uniqueName == CHEWING_IME }
        val current = currentIme()
        currentIme = current.uniqueName
        currentImeLabel = current.nativeName.ifBlank { current.displayName }
        publishState()
    }

    private fun toggleModifier(modifier: Modifier) {
        if (!modifiers.remove(modifier)) modifiers.add(modifier)
    }

    private fun clearOneShotModifiers() {
        modifiers.remove(Modifier.SHIFT)
        modifiers.remove(Modifier.CTRL)
        modifiers.remove(Modifier.ALT)
        modifiers.remove(Modifier.META)
    }

    private fun currentKeyStates(): UInt {
        // Deliberately omit KeyState.Virtual: this panel emulates a full
        // external keyboard. Fcitx still composes Chewing input, while keys
        // it does not consume (F-keys, arrows, Ctrl chords) are forwarded to
        // the target editor with physical-key semantics.
        var states = KeyState.NoState.state
        if (modifiers.contains(Modifier.SHIFT)) states = states or KeyState.Shift.state
        if (modifiers.contains(Modifier.CAPS)) states = states or KeyState.CapsLock.state
        if (modifiers.contains(Modifier.CTRL)) states = states or KeyState.Ctrl.state
        if (modifiers.contains(Modifier.ALT)) states = states or KeyState.Alt.state
        if (modifiers.contains(Modifier.META)) states = states or KeyState.Meta.state
        return states
    }

    private fun publishState() {
        val preedit = clientPreedit.ifBlank { panelPreedit }
        stateConsumer.accept(
            DuoDeckEngineState(
                preedit,
                candidates,
                currentImeLabel,
                currentIme == CHEWING_IME,
                chewingAvailable
            )
        )
    }

    private fun enqueue(operation: suspend FcitxAPI.() -> Unit) {
        if (!closed) operations.trySend(operation)
    }

    private fun toast(message: Int) {
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun specialKeyName(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_ESCAPE -> "Escape"
        KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
        KeyEvent.KEYCODE_DEL -> "BackSpace"
        KeyEvent.KEYCODE_TAB -> "Tab"
        KeyEvent.KEYCODE_ENTER -> "Return"
        KeyEvent.KEYCODE_DPAD_UP -> "Up"
        KeyEvent.KEYCODE_DPAD_DOWN -> "Down"
        KeyEvent.KEYCODE_DPAD_LEFT -> "Left"
        KeyEvent.KEYCODE_DPAD_RIGHT -> "Right"
        KeyEvent.KEYCODE_MENU -> "Menu"
        KeyEvent.KEYCODE_FUNCTION -> "XF86Fn"
        in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 ->
            "F${keyCode - KeyEvent.KEYCODE_F1 + 1}"
        else -> null
    }

    private companion object {
        const val PANEL_CLIENT_NAME = "dev.duodeck.DuoDeckPanel"
        const val CHEWING_IME = "chewing"
        const val ENGLISH_IME = "keyboard-us"
    }
}
