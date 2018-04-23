package com.kprotty.port.core;

public abstract class AutoCloseableThread extends Thread implements AutoCloseable {

    public AutoCloseableThread(final String name) {
        super(name);
        setPriority(Thread.MAX_PRIORITY);
    }

    public void closeAndJoin() throws Exception {
        close();
        join();
    }
}
