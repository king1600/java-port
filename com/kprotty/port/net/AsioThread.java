package com.kprotty.port.net;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import com.kprotty.port.core.Runtime;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import com.kprotty.port.core.AutoCloseableThread;

public final class AsioThread extends AutoCloseableThread {
    public static final int DefaultAmount = 2;

    private final int id;
    private Selector selector;
    private final Runtime runtime;
    private volatile boolean running;

    public AsioThread(final Runtime runtime, final int id) {
        super(runtime.getName() + ".asio." + id);
        this.id = id;
        this.running = false;
        this.runtime = runtime;
        try {
            selector = SelectorProvider.provider().openSelector();
        } catch (IOException ex) {
            selector = null;
        }
    }

    public final Selector getSelector() {
        return selector;
    }

    @Override
    public void close() throws Exception {
        running = false;
        selector.wakeup();
        try { join(1000); } catch (InterruptedException ex) {}
        selector.close();
    }

    @Override
    public void run() {
        try {
            if (selector == null)
                throw new IOException("Selector failed to initialize");
            running = true;

            while (running) {
                selector.select();
                final Iterator<SelectionKey> events = selector.selectedKeys().iterator();

                while (events.hasNext()) {
                    final SelectionKey event = events.next();
                    events.remove();
                    final AsioEvent asioEvent = (AsioEvent)event.attachment();
                    if (asioEvent == null || !event.isValid()) continue;

                    if (event.isAcceptable()) asioEvent.eventAccept(selector, event);
                    else if (event.isConnectable()) asioEvent.eventConnect(event);
                    else if (event.isWritable()) asioEvent.eventWrite(event);
                    else if (event.isReadable()) asioEvent.eventRead(event);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
