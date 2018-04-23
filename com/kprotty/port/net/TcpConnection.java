package com.kprotty.port.net;

import java.net.Socket;
import java.io.IOException;
import java.net.InetSocketAddress;

import com.kprotty.port.core.Behaviour;
import com.kprotty.port.core.Runtime;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public final class TcpConnection extends AsioEvent {
    private boolean isOpen;
    private boolean isConnected;
    private ByteBuffer readBuffer;

    private final AsioBuffer buffer;
    private final SelectionKey event;
    private final SocketChannel client;
    private final TcpConnectionNotify notifier;

    public TcpConnection(final String host, final int port, final TcpConnectionNotify notify) throws IOException {
        this(Runtime.Default, host, port, notify);
    }

    public TcpConnection(final TcpListener listener, final SocketChannel channel, final SelectionKey event, final TcpConnectionNotify notify) throws IOException {
        super(listener.getRuntime());

        this.event = event;
        this.client = channel;
        this.notifier = notify;
        this.buffer = new AsioBuffer();
        this.isOpen = this.isConnected = true;

        initalizeReadBuffer();
    }

    public TcpConnection(final Runtime runtime, final String host, final int port, final TcpConnectionNotify notify) throws IOException {
        super(runtime);

        isOpen = true;
        notifier = notify;
        isConnected = false;
        buffer = new AsioBuffer();
        client = SocketChannel.open();

        final Socket socket = client.socket();
        socket.setTcpNoDelay(true);
        socket.setPerformancePreferences(1, 2, 0);
        event = client.register(runtime.getAsioThread().getSelector(), SelectionKey.OP_CONNECT | SelectionKey.OP_READ);

        client.connect(new InetSocketAddress(host, port));
    }

    private void initalizeReadBuffer() {
        try {
            readBuffer = ByteBuffer.allocate(client.socket().getReceiveBufferSize());
        } catch (IOException ex) {
            readBuffer = ByteBuffer.allocate(4096);
        }
    }

    @Override
    public void eventRead(final SelectionKey event) {
        send(this::read);
    }

    @Override
    public void eventConnect(final SelectionKey event) {
        send(this::connect);
        initalizeReadBuffer();
    }

    @Behaviour
    public void close(Object... args) {
        try {
            client.close();
            event.cancel();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (isOpen) {
            isOpen = false;
            if (isConnected) notifier.closed(this); else notifier.connectFailed(this);
            isConnected = false;
        }
    }

    @Behaviour
    private void connect(Object... args) {
        isConnected = true;
        notifier.connected(this);
    }

    @Behaviour
    private void read(Object... args) {
        int bytesRead = 0;

        try {
            while ((bytesRead = client.read(readBuffer)) > 0) {
                buffer.write(readBuffer.array(), 0, bytesRead);
                readBuffer.clear();
            }
            if (buffer.size() > 0)
                notifier.received(this, buffer);
        } catch (IOException ex) {
            close();
        } finally {
            if (bytesRead == -1)
                close();
        }
    }

    @Behaviour
    public void write(Object... args) {
        ByteBuffer writeBuffer;
        if (args == null) return;

        for (final Object data : args) {
            if (data == null) continue;
            else if (data instanceof byte[]) {
                writeBuffer = ByteBuffer.wrap((byte[]) data);
            } else if (data instanceof String) {
                writeBuffer = ByteBuffer.wrap(((String) data).getBytes());
            } else if (data instanceof AsioBuffer.Segment) {
                final AsioBuffer.Segment segment = (AsioBuffer.Segment) data;
                writeBuffer = ByteBuffer.wrap(segment.getBytes(), segment.offset, segment.length);
            } else writeBuffer = ByteBuffer.wrap(data.toString().getBytes());

            try {
                while (writeBuffer.hasRemaining()) client.write(writeBuffer);
            } catch (IOException ex) {
                close();
                return;
            }
        }
    }

}
