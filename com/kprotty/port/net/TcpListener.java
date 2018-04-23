package com.kprotty.port.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetSocketAddress;

import com.kprotty.port.core.Behaviour;
import com.kprotty.port.core.Runtime;

import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public final class TcpListener extends AsioEvent {
    private boolean isOpen;
    private final SelectionKey event;
    private final TcpListenerNotify notifier;
    private final ServerSocketChannel server;

    public TcpListener(final int port, final TcpListenerNotify notify) throws IOException {
        this(Runtime.Default, null, port, notify);
    }

    public TcpListener(final Runtime runtime, final int port, final TcpListenerNotify notify) throws IOException {
        this(runtime, null, port, notify);
    }

    public TcpListener(final String host, final int port, final TcpListenerNotify notify) throws IOException {
        this(Runtime.Default, host, port, notify);
    }

    public TcpListener(final Runtime runtime, final String host, final int port, final TcpListenerNotify notify) throws IOException {
        super(runtime);

        isOpen = true;
        notifier = notify;
        server = ServerSocketChannel.open();
        final ServerSocket socket = server.socket();

        socket.setReuseAddress(true);
        socket.setPerformancePreferences(2, 1, 0);
        socket.bind(host == null ? new InetSocketAddress(port) : new InetSocketAddress(host, port));
        server.configureBlocking(false);

        event = server.register(runtime.getAsioThread().getSelector(), SelectionKey.OP_ACCEPT);
        event.attach(this);
        notifier.listening(this);
    }

    public ServerSocket getSocket() {
        return server.socket();
    }

    @Behaviour
    public void close(Object... args) {
        try {
            server.close();
            event.cancel();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (isOpen) {
            notifier.closed(this);
            isOpen = false;
        }
    }

    @Override
    public void eventAccept(final Selector selector, final SelectionKey _event) {
        try {
            final SocketChannel channel = server.accept();
            final Socket socket = channel.socket();

            socket.setTcpNoDelay(true);
            socket.setPerformancePreferences(0, 2, 1);
            channel.configureBlocking(false);

            final TcpConnectionNotify notify = notifier.accepted(this);
            if (notify != null) {
                final SelectionKey channelEvent = channel.register(selector, SelectionKey.OP_READ);
                channelEvent.attach(new TcpConnection(this, channel, channelEvent, notify));
            } else channel.close();

        } catch (IOException ex) {
            ex.printStackTrace();
            close();
        }
    }
}
