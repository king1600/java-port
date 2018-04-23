package com.kprotty.port.net;

public interface TcpConnectionNotify {

    void closed(final TcpConnection connection);

    default void connected(final TcpConnection connection) {}

    default void connectFailed(final TcpConnection connection) {}

    void received(final TcpConnection connection, final AsioBuffer buffer);

    default AsioBuffer sent(final TcpConnection connection, final AsioBuffer buffer) { return buffer; }

}
