package com.kprotty.port.net;

public interface TcpListenerNotify {

    void closed(final TcpListener listener);

    void listening(final TcpListener listener);

    TcpConnectionNotify accepted(final TcpListener listener);

}
