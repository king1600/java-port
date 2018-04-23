package com.kprotty.port.core;

import java.util.Queue;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Scheduler extends AutoCloseableThread {
    protected final int id;
    private final Runtime runtime;
    private volatile boolean isRunning;
    private final BlockingEvent isActive;
    private final Queue<Actor> actorQueue;

    public static final int DefaultAmount = java.lang.Runtime.getRuntime().availableProcessors();

    public Scheduler(final Runtime runtime, final int id) {
        super(runtime.getName() + "." + id);

        this.id = id;
        this.isRunning = false;
        this.runtime = runtime;
        this.isActive = new BlockingEvent();
        this.actorQueue = new ConcurrentLinkedQueue<>();

        runtime.addIdle(this);
    }

    protected void schedule(final Actor actor) {
        actorQueue.add(actor);
        isActive.set();
    }

    private final Actor getActor() {
        Actor actor = actorQueue.poll();
        if (actor != null) return actor;
        for (final Iterator<Scheduler> it = runtime.schedulers.iterator(); it.hasNext();)
            if ((actor = it.next().actorQueue.poll()) != null)
                return actor;
        return actorQueue.poll();
    }

    @Override
    public void close() throws InterruptedException {
        isRunning = false;
        isActive.set();
        join();
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            final Actor actor = getActor();
            if (actor != null) {
                final Actor.Message message = actor.mailBox.poll();
                if (message == null) {
                    actor.setInactive();
                } else {
                    message.run();
                    runtime.schedule(actor);
                }
                continue;
            }
            isActive.sleep();
        }
    }
}
