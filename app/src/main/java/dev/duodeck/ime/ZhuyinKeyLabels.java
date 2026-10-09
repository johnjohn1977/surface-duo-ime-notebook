/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Visual legends for Chewing's default DaChen (大千) keyboard layout. */
public final class ZhuyinKeyLabels {
    private static final Map<String, String> BY_KEY_ID;

    static {
        Map<String, String> labels = new HashMap<>();

        labels.put("number-1", "ㄅ");
        labels.put("letter-q", "ㄆ");
        labels.put("letter-a", "ㄇ");
        labels.put("letter-z", "ㄈ");

        labels.put("number-2", "ㄉ");
        labels.put("letter-w", "ㄊ");
        labels.put("letter-s", "ㄋ");
        labels.put("letter-x", "ㄌ");

        labels.put("letter-e", "ㄍ");
        labels.put("letter-d", "ㄎ");
        labels.put("letter-c", "ㄏ");

        labels.put("letter-r", "ㄐ");
        labels.put("letter-f", "ㄑ");
        labels.put("letter-v", "ㄒ");

        labels.put("number-5", "ㄓ");
        labels.put("letter-t", "ㄔ");
        labels.put("letter-g", "ㄕ");
        labels.put("letter-b", "ㄖ");

        labels.put("letter-y", "ㄗ");
        labels.put("letter-h", "ㄘ");
        labels.put("letter-n", "ㄙ");

        labels.put("letter-u", "ㄧ");
        labels.put("letter-j", "ㄨ");
        labels.put("letter-m", "ㄩ");

        labels.put("number-8", "ㄚ");
        labels.put("letter-i", "ㄛ");
        labels.put("letter-k", "ㄜ");
        labels.put("comma", "ㄝ");

        labels.put("number-9", "ㄞ");
        labels.put("letter-o", "ㄟ");
        labels.put("letter-l", "ㄠ");
        labels.put("period", "ㄡ");

        labels.put("number-10", "ㄢ");
        labels.put("letter-p", "ㄣ");
        labels.put("semicolon", "ㄤ");
        labels.put("slash", "ㄥ");
        labels.put("number-11", "ㄦ");

        labels.put("number-6", "ˊ");
        labels.put("number-3", "ˇ");
        labels.put("number-4", "ˋ");
        labels.put("number-7", "˙");

        BY_KEY_ID = Collections.unmodifiableMap(labels);
    }

    private ZhuyinKeyLabels() {
    }

    public static String forKey(NotebookLayoutSpec.Key key) {
        return key == null ? null : BY_KEY_ID.get(key.id);
    }

    static int mappedKeyCount() {
        return BY_KEY_ID.size();
    }
}
