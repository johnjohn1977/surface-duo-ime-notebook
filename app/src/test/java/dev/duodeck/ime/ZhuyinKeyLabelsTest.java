/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class ZhuyinKeyLabelsTest {
    @Test
    public void defaultDaChenLayoutMatchesChewingKeyPositions() {
        assertRow("ㄅㄉˇˋㄓˊ˙ㄚㄞㄢㄦ", "number-1", "number-2", "number-3",
                "number-4", "number-5", "number-6", "number-7", "number-8",
                "number-9", "number-10", "number-11");
        assertRow("ㄆㄊㄍㄐㄔㄗㄧㄛㄟㄣ", "letter-q", "letter-w", "letter-e",
                "letter-r", "letter-t", "letter-y", "letter-u", "letter-i",
                "letter-o", "letter-p");
        assertRow("ㄇㄋㄎㄑㄕㄘㄨㄜㄠㄤ", "letter-a", "letter-s", "letter-d",
                "letter-f", "letter-g", "letter-h", "letter-j", "letter-k",
                "letter-l", "semicolon");
        assertRow("ㄈㄌㄏㄒㄖㄙㄩㄝㄡㄥ", "letter-z", "letter-x", "letter-c",
                "letter-v", "letter-b", "letter-n", "letter-m", "comma",
                "period", "slash");
        assertEquals(41, ZhuyinKeyLabels.mappedKeyCount());
    }

    @Test
    public void nonPhoneticNotebookKeysHaveNoChineseLegend() {
        assertNull(ZhuyinKeyLabels.forKey(find("space")));
        assertNull(ZhuyinKeyLabels.forKey(find("number-0")));
        assertNull(ZhuyinKeyLabels.forKey(find("number-12")));
    }

    private void assertRow(String expected, String... keyIds) {
        StringBuilder actual = new StringBuilder();
        for (String keyId : keyIds) {
            actual.append(ZhuyinKeyLabels.forKey(find(keyId)));
        }
        assertEquals(expected, actual.toString());
    }

    private NotebookLayoutSpec.Key find(String id) {
        for (NotebookLayoutSpec.Key key : NotebookLayoutSpec.keys()) {
            if (id.equals(key.id)) return key;
        }
        throw new AssertionError("Missing key " + id);
    }
}
