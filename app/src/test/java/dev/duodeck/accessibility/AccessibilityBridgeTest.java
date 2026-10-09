/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.accessibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import dev.duodeck.geometry.FloatPoint;

public final class AccessibilityBridgeTest {
    @After
    public void clearBridge() {
        AccessibilityBridge.Registration registration =
                AccessibilityBridge.register(new FakeDispatcher());
        registration.close();
    }

    @Test
    public void staleRegistrationCannotClearNewService() {
        FakeDispatcher first = new FakeDispatcher();
        FakeDispatcher second = new FakeDispatcher();
        AccessibilityBridge.Registration firstRegistration = AccessibilityBridge.register(first);
        AccessibilityBridge.Registration secondRegistration = AccessibilityBridge.register(second);

        firstRegistration.close();

        assertSame(second, AccessibilityBridge.current());
        secondRegistration.close();
    }

    @Test
    public void observerReceivesCurrentAndFutureConnectionState() {
        List<Boolean> states = new ArrayList<>();
        AccessibilityBridge.Registration observer =
                AccessibilityBridge.observeConnection(states::add);
        AccessibilityBridge.Registration service =
                AccessibilityBridge.register(new FakeDispatcher());

        service.close();
        observer.close();

        assertEquals(List.of(false, true, false), states);
    }

    private static final class FakeDispatcher implements GestureDispatcher {
        @Override
        public FloatPoint pointerPosition() {
            return new FloatPoint(0, 0);
        }

        @Override
        public void movePointer(float deltaX, float deltaY) {
        }

        @Override
        public boolean tap() {
            return true;
        }

        @Override
        public boolean longPress() {
            return true;
        }

        @Override
        public boolean drag(FloatPoint from, FloatPoint to) {
            return true;
        }

        @Override
        public boolean scroll(float deltaY) {
            return true;
        }
    }
}
