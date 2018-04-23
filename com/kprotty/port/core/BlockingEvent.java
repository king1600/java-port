package com.kprotty.port.core;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public final class BlockingEvent {
    private volatile int state;
    private static final int Enabled = 1;
    private static final int Disabled = 0;
    private static final AtomicIntegerFieldUpdater Event =
            AtomicIntegerFieldUpdater.newUpdater(BlockingEvent.class, "state");

    public BlockingEvent() {
        state = Disabled;
    }

    public final boolean isSet() {
        return Event.get(this) == Enabled;
    }

    public void set() {
        if (Event.compareAndSet(this, Enabled, Disabled))
            synchronized(this) { this.notifyAll(); }
    }

    public void sleep() {
        if (Event.compareAndSet(this, Disabled, Enabled))
            try { synchronized(this) { this.wait(); } } catch (InterruptedException ex) {}
    }
}
