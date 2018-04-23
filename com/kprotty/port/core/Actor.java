package com.kprotty.port.core;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class Actor {

    protected static final class Message implements Runnable {
        private final Object[] args;
        private final Consumer<Object[]> method;

        public Message(final Consumer<Object[]> method, final Object[] args) {
            this.args = args;
            this.method = method;
        }

        @Override
        public void run() {
            method.accept(args);
        }
    }

    private final Runtime runtime;
    private volatile int isActive;
    protected final Queue<Message> mailBox;
    private static final AtomicIntegerFieldUpdater Active =
            AtomicIntegerFieldUpdater.newUpdater(Actor.class, "isActive");

    public Actor() {
        this(Runtime.Default);
    }

    public Actor(final Runtime runtime) {
        this.isActive = 0;
        this.runtime = runtime;
        this.mailBox = new ConcurrentLinkedQueue<>();
    }

    public final Runtime getRuntime() {
        return runtime;
    }

    protected void setInactive() {
        Active.set(this, 0);
    }

    public void send(final Consumer<Object[]> method, Object... args) {
        mailBox.add(new Message(method, args));
        if (Active.compareAndSet(this, 0, 1))
            runtime.schedule(this);
    }
}
