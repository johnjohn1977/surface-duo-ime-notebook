/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime;

import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class NotebookLayoutSpec {
    public static final int ROWS = 6;
    public static final int COLUMNS = 15;

    private NotebookLayoutSpec() {
    }

    public static List<Key> keys() {
        List<Key> keys = new ArrayList<>();

        addSpecial(keys, "esc", "Esc", 0, 0, 1, KeyEvent.KEYCODE_ESCAPE);
        for (int index = 0; index < 12; index++) {
            addSpecial(keys, "f" + (index + 1), "F" + (index + 1), 0, index + 1, 1,
                    KeyEvent.KEYCODE_F1 + index);
        }
        addSpecial(keys, "delete", "Delete", 0, 13, 2, KeyEvent.KEYCODE_FORWARD_DEL);

        String[] numberLabels = {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "="};
        String[] numberShift = {"~", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+"};
        for (int index = 0; index < numberLabels.length; index++) {
            addText(keys, "number-" + index, numberLabels[index], numberShift[index], 1, index, 1);
        }
        addSpecial(keys, "backspace", "Backspace", 1, 13, 2, KeyEvent.KEYCODE_DEL);

        addSpecial(keys, "tab", "Tab", 2, 0, 2, KeyEvent.KEYCODE_TAB);
        String qwerty = "qwertyuiop";
        for (int index = 0; index < qwerty.length(); index++) {
            String value = String.valueOf(qwerty.charAt(index));
            String uppercase = value.toUpperCase(Locale.ROOT);
            addText(keys, "letter-" + value, value, uppercase, 2, index + 2, 1);
        }
        addText(keys, "left-bracket", "[", "{", 2, 12, 1);
        addText(keys, "right-bracket", "]", "}", 2, 13, 1);
        addText(keys, "backslash", "\\", "|", 2, 14, 1);

        addModifier(keys, "caps", "Caps", 3, 0, 2, Modifier.CAPS);
        String home = "asdfghjkl";
        for (int index = 0; index < home.length(); index++) {
            String value = String.valueOf(home.charAt(index));
            String uppercase = value.toUpperCase(Locale.ROOT);
            addText(keys, "letter-" + value, value, uppercase, 3, index + 2, 1);
        }
        addText(keys, "semicolon", ";", ":", 3, 11, 1);
        addText(keys, "quote", "'", "\"", 3, 12, 1);
        addSpecial(keys, "enter-main", "Enter", 3, 13, 2, KeyEvent.KEYCODE_ENTER);

        addModifier(keys, "shift-left", "Shift", 4, 0, 2, Modifier.SHIFT);
        String bottom = "zxcvbnm";
        for (int index = 0; index < bottom.length(); index++) {
            String value = String.valueOf(bottom.charAt(index));
            String uppercase = value.toUpperCase(Locale.ROOT);
            addText(keys, "letter-" + value, value, uppercase, 4, index + 2, 1);
        }
        addText(keys, "comma", ",", "<", 4, 9, 1);
        addText(keys, "period", ".", ">", 4, 10, 1);
        addText(keys, "slash", "/", "?", 4, 11, 1);
        addSpecial(keys, "arrow-up", "↑", 4, 12, 1, KeyEvent.KEYCODE_DPAD_UP);
        addModifier(keys, "shift-right", "Shift", 4, 13, 1, Modifier.SHIFT);
        addSpecial(keys, "enter-lower", "↵", 4, 14, 1, KeyEvent.KEYCODE_ENTER);

        addModifier(keys, "ctrl", "Ctrl", 5, 0, 1, Modifier.CTRL);
        addSpecial(keys, "fn", "Fn", 5, 1, 1, KeyEvent.KEYCODE_FUNCTION);
        addModifier(keys, "meta", "◆", 5, 2, 1, Modifier.META);
        addModifier(keys, "alt-left", "Alt", 5, 3, 1, Modifier.ALT);
        addText(keys, "space", "", "", 5, 4, 5, " ");
        addModifier(keys, "alt-right", "Alt", 5, 9, 1, Modifier.ALT);
        addSpecial(keys, "menu", "Menu", 5, 10, 1, KeyEvent.KEYCODE_MENU);
        addSpecial(keys, "arrow-left", "←", 5, 11, 1, KeyEvent.KEYCODE_DPAD_LEFT);
        addSpecial(keys, "arrow-down", "↓", 5, 12, 1, KeyEvent.KEYCODE_DPAD_DOWN);
        addSpecial(keys, "arrow-right", "→", 5, 13, 1, KeyEvent.KEYCODE_DPAD_RIGHT);
        keys.add(Key.hide("hide", "⌄", 5, 14, 1));

        return Collections.unmodifiableList(keys);
    }

    private static void addText(
            List<Key> keys, String id, String normal, String shifted, int row, int column, int span) {
        addText(keys, id, normal, shifted, row, column, span, normal.toLowerCase(Locale.ROOT));
    }

    private static void addText(
            List<Key> keys,
            String id,
            String normal,
            String shifted,
            int row,
            int column,
            int span,
            String committed) {
        keys.add(Key.text(id, normal, shifted, committed, row, column, span));
    }

    private static void addSpecial(
            List<Key> keys, String id, String label, int row, int column, int span, int keyCode) {
        keys.add(Key.special(id, label, row, column, span, keyCode));
    }

    private static void addModifier(
            List<Key> keys, String id, String label, int row, int column, int span, Modifier modifier) {
        keys.add(Key.modifier(id, label, row, column, span, modifier));
    }

    public enum Kind {
        TEXT,
        SPECIAL,
        MODIFIER,
        HIDE
    }

    public enum Modifier {
        SHIFT,
        CAPS,
        CTRL,
        ALT,
        META
    }

    public static final class Key {
        public final String id;
        public final String label;
        public final String shiftedLabel;
        public final String committedText;
        public final int row;
        public final int column;
        public final int span;
        public final Kind kind;
        public final int keyCode;
        public final Modifier modifier;

        private Key(
                String id,
                String label,
                String shiftedLabel,
                String committedText,
                int row,
                int column,
                int span,
                Kind kind,
                int keyCode,
                Modifier modifier) {
            this.id = id;
            this.label = label;
            this.shiftedLabel = shiftedLabel;
            this.committedText = committedText;
            this.row = row;
            this.column = column;
            this.span = span;
            this.kind = kind;
            this.keyCode = keyCode;
            this.modifier = modifier;
        }

        static Key text(
                String id,
                String label,
                String shiftedLabel,
                String committedText,
                int row,
                int column,
                int span) {
            return new Key(id, label, shiftedLabel, committedText, row, column, span,
                    Kind.TEXT, KeyEvent.KEYCODE_UNKNOWN, null);
        }

        static Key special(String id, String label, int row, int column, int span, int keyCode) {
            return new Key(id, label, label, null, row, column, span,
                    Kind.SPECIAL, keyCode, null);
        }

        static Key modifier(
                String id, String label, int row, int column, int span, Modifier modifier) {
            return new Key(id, label, label, null, row, column, span,
                    Kind.MODIFIER, KeyEvent.KEYCODE_UNKNOWN, modifier);
        }

        public String committedText(boolean shiftActive, boolean capsActive) {
            if (kind != Kind.TEXT) {
                return null;
            }
            if (usesShiftedValue(shiftActive, capsActive)) {
                return shiftedLabel;
            }
            return committedText;
        }

        public String displayLabel(boolean shiftActive, boolean capsActive) {
            if (kind == Kind.TEXT && usesShiftedValue(shiftActive, capsActive)) {
                return shiftedLabel;
            }
            return label;
        }

        private boolean usesShiftedValue(boolean shiftActive, boolean capsActive) {
            boolean isLetter = id.startsWith("letter-");
            boolean shifted = isLetter ? shiftActive ^ capsActive : shiftActive;
            return shifted && shiftedLabel != null && !shiftedLabel.isEmpty();
        }

        static Key hide(String id, String label, int row, int column, int span) {
            return new Key(id, label, label, null, row, column, span,
                    Kind.HIDE, KeyEvent.KEYCODE_UNKNOWN, null);
        }
    }
}
