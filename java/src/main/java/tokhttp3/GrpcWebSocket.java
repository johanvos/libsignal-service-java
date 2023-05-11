package tokhttp3;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.logging.Logger;
import okio.ByteString;
import io.privacyresearch.grpcproxy.client.TunnelClient;

/**
 *
 * @author johan
 */
public class GrpcWebSocket implements WebSocket {

    private static final Logger LOG = Logger.getLogger(GrpcWebSocket.class.getName());

    public GrpcWebSocket (WebSocketListener listener) {
                ChannelCredentials creds = InsecureChannelCredentials.create();
        ManagedChannel channel = Grpc.newChannelBuilder("localhost:50051", creds).build();
        TunnelClient tunnelClient = new TunnelClient(channel);
    }

    @Override
    public boolean close(int code, String reason) {
        LOG.severe("We are asked to close this websocket, reason = "+reason+" and code = "+code);
        return true;
    }

    @Override
    public boolean send(ByteString of) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean send(String text) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public long queueSize() {
        return 1;
    }
    
}
