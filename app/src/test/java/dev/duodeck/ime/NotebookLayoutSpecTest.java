/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.ime;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class NotebookLayoutSpecTest {
    @Test
    public void everyCellIsCoveredExactlyOnce() {
        boolean[][] occupied = new boolean[NotebookLayoutSpec.ROWS][NotebookLayoutSpec.COLUMNS];

        for (NotebookLayoutSpec.Key key : NotebookLayoutSpec.keys()) {
            for (int column = key.column; column < key.column + key.span; column++) {
                assertEquals("overlap at " + key.id, false, occupied[key.row][column]);
                occupied[key.row][column] = true;
            }
        }
        for (int row = 0; row < occupied.length; row++) {
            for (int column = 0; column < occupied[row].length; column++) {
                assertEquals("gap at " + row + "," + column, true, occupied[row][column]);
            }
        }
    }

    @Test
    public void arrowsStayFullSizeAndUpIsAboveDown() {
        NotebookLayoutSpec.Key up = find("arrow-up");
        NotebookLayoutSpec.Key down = find("arrow-down");
        assertEquals(1, up.span);
        assertEquals(1, down.span);
        assertEquals(down.column, up.column);
        assertEquals(down.row - 1, up.row);
    }

    @Test
    public void capsOnlyChangesLettersWhileShiftChangesSymbols() {
        NotebookLayoutSpec.Key letter = find("letter-a");
        NotebookLayoutSpec.Key number = find("number-2");
        assertEquals("a", letter.committedText(false, false));
        assertEquals("A", letter.committedText(false, true));
        assertEquals("2", number.committedText(false, true));
        assertEquals("@", number.committedText(true, true));
    }

    @Test
    public void letterLegendFollowsNormalShiftAndCapsRules() {
        NotebookLayoutSpec.Key letter = find("letter-a");

        assertEquals("a", letter.displayLabel(false, false));
        assertEquals("A", letter.displayLabel(true, false));
        assertEquals("A", letter.displayLabel(false, true));
        assertEquals("a", letter.displayLabel(true, true));
    }

    private NotebookLayoutSpec.Key find(String id) {
        for (NotebookLayoutSpec.Key key : NotebookLayoutSpec.keys()) {
            if (id.equals(key.id)) return key;
        }
        throw new AssertionError("Missing key " + id);
    }
}
