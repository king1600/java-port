package com.kprotty.port.net;

import com.kprotty.port.core.Actor;
import com.kprotty.port.core.Runtime;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public abstract class AsioEvent extends Actor {

    public AsioEvent(final Runtime runtime) {
        super(runtime);
    }

    void eventRead(final SelectionKey event) {}

    void eventConnect(final SelectionKey event) {}

    void eventAccept(final Selector selector, final SelectionKey event) {}

    void eventWrite(final SelectionKey event) {
        event.interestOps(event.interestOps() & ~SelectionKey.OP_WRITE);
    }

}
