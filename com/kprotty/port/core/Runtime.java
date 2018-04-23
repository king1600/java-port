package com.kprotty.port.core;

import com.kprotty.port.net.AsioThread;

import java.util.Queue;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class Runtime implements Runnable, AutoCloseable {
    public static Runtime Default = new Runtime();

    private final String name;
    private MainActor mainActor;
    private final BlockingEvent isRunning;
    private final Queue<Scheduler> idleSchedulers;
    protected final CycleIterator<Scheduler> schedulers;
    protected final CycleIterator<AsioThread> asioThreads;

    public Runtime() {
        this(null);
    }

    public Runtime(String name) {
        mainActor = null;
        isRunning = new BlockingEvent();
        schedulers = new CycleIterator<>();
        asioThreads = new CycleIterator<>();
        idleSchedulers = new ConcurrentLinkedQueue<>();
        this.name = name != null ? name : this.getClass().getCanonicalName();
    }

    public final String getName() {
        return name;
    }

    public final AsioThread getAsioThread() {
        return asioThreads.get();
    }

    protected void addIdle(final Scheduler scheduler) {
        idleSchedulers.add(scheduler);
    }

    public void schedule(final Actor actor) {
        Scheduler scheduler = idleSchedulers.poll();
        if (scheduler == null)
            scheduler = schedulers.get();
        scheduler.schedule(actor);
    }

    public Runtime setMain(final MainActor actor) {
        mainActor = actor;
        return this;
    }

    public void start() {
        start(Scheduler.DefaultAmount);
    }

    public void start(final int schedulers) {
        start(null, schedulers, AsioThread.DefaultAmount);
    }

    public void start(final int schedulers, final int asioThreads) { start(null, schedulers, asioThreads); }

    public void start(final String[] args, final int schedulers) { start(args, schedulers, AsioThread.DefaultAmount); }

    public void start(final String[] args, int numSchedulers, int numAsioThreads) {
        while (numSchedulers-- != 0) addScheduler();
        while (numAsioThreads-- != 0) addAsioThread();
        if (mainActor != null) mainActor.start(args);
        run();
    }

    public final AsioThread addAsioThread() {
        final AsioThread thread = new AsioThread(this, asioThreads.size());
        asioThreads.add(thread);
        if (isRunning.isSet())
            thread.start();
        return thread;
    }

    public final Scheduler addScheduler() {
        final Scheduler thread = new Scheduler(this, schedulers.size());
        schedulers.add(thread);
        if (isRunning.isSet())
            thread.start();
        return thread;
    }

    @Override
    public void run() {
        for (final Iterator<Scheduler> it = schedulers.iterator(); it.hasNext();)
            it.next().start();
        for (final Iterator<AsioThread> it = asioThreads.iterator(); it.hasNext();)
            it.next().start();
        isRunning.sleep();
    }

    @Override
    public void close() {
        try {
            for (final Iterator<Scheduler> it = schedulers.iterator(); it.hasNext();)
                it.next().close();
            for (final Iterator<AsioThread> it = asioThreads.iterator(); it.hasNext();)
                it.next().close();
        } catch (Exception ex) {
            ex.printStackTrace();
            for (final Iterator<Scheduler> it = schedulers.iterator(); it.hasNext();)
                it.next().interrupt();
            for (final Iterator<AsioThread> it = asioThreads.iterator(); it.hasNext();)
                it.next().interrupt();
        }
        isRunning.set();
    }
}
