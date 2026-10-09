/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Surface Duo IME Notebook Contributors
 */

package dev.duodeck.accessibility;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AccessibilityBridge {
    private static final AtomicReference<Entry> CURRENT = new AtomicReference<>();
    private static final CopyOnWriteArrayList<ConnectionListener> CONNECTION_LISTENERS =
            new CopyOnWriteArrayList<>();

    private AccessibilityBridge() {
    }

    public static Registration register(GestureDispatcher dispatcher) {
        Object token = new Object();
        CURRENT.set(new Entry(token, dispatcher));
        notifyConnectionChanged(true);
        return () -> unregister(token);
    }

    private static void unregister(Object token) {
        Entry current = CURRENT.get();
        if (current != null && current.token == token) {
            if (CURRENT.compareAndSet(current, null)) {
                notifyConnectionChanged(false);
            }
        }
    }

    public static GestureDispatcher current() {
        Entry entry = CURRENT.get();
        return entry == null ? null : entry.dispatcher;
    }

    public static Registration observeConnection(ConnectionListener listener) {
        CONNECTION_LISTENERS.add(listener);
        notifyListener(listener, CURRENT.get() != null);
        return () -> CONNECTION_LISTENERS.remove(listener);
    }

    private static void notifyConnectionChanged(boolean connected) {
        for (ConnectionListener listener : CONNECTION_LISTENERS) {
            notifyListener(listener, connected);
        }
    }

    private static void notifyListener(ConnectionListener listener, boolean connected) {
        try {
            listener.onConnectionChanged(connected);
        } catch (RuntimeException ignored) {
            // A detached UI observer must not break the service registration.
        }
    }

    private static final class Entry {
        final Object token;
        final GestureDispatcher dispatcher;

        Entry(Object token, GestureDispatcher dispatcher) {
            this.token = token;
            this.dispatcher = dispatcher;
        }
    }

    public interface Registration {
        void close();
    }

    public interface ConnectionListener {
        void onConnectionChanged(boolean connected);
    }
}
