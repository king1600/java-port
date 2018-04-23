
import java.io.IOException;
import com.kprotty.port.core.Behaviour;
import com.kprotty.port.core.Runtime;
import com.kprotty.port.core.MainActor;
import com.kprotty.port.net.*;

public class Test extends MainActor implements TcpListenerNotify {

    private static final class TestClient implements TcpConnectionNotify {

        @Override
        public void closed(final TcpConnection connection) { }

        @Override
        public void received(final TcpConnection connection, final AsioBuffer buffer) {
            try (final AsioBuffer.Segment data = buffer.read(buffer.size())) {
                connection.write("HTTP/1.1 200 Ok\r\nContent-Length: 11\r\n\r\nHello world");
            }
        }
    }

    @Override
    public void closed(TcpListener listener) {
        System.out.println("server closing...");
    }

    @Override
    public void listening(TcpListener listener) {
        System.out.println("server started on " + listener.getSocket().getLocalSocketAddress());
    }

    @Override
    public TcpConnectionNotify accepted(TcpListener listener) {
        return new TestClient();
    }

    public static void main(final String[] args) {
       Runtime.Default.setMain(new Test()).start(args, 2, 1);
    }

    @Behaviour
    public void start(final String[] args) {
        try {
            new TcpListener(8080, this);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
