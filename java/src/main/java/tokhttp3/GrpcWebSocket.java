package tokhttp3;

import io.grpc.stub.StreamObserver;
import io.privacyresearch.grpcproxy.SignalRpcMessage;
import io.privacyresearch.grpcproxy.SignalRpcReply;
import java.util.logging.Logger;
import okio.ByteString;
import io.privacyresearch.grpcproxy.client.GrpcConfig;
import io.privacyresearch.grpcproxy.client.StreamListener;
import io.privacyresearch.grpcproxy.client.TunnelClient;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 *
 * @author johan
 */
public class GrpcWebSocket implements WebSocket {

    private static final Logger LOG = Logger.getLogger(GrpcWebSocket.class.getName());
    private final TunnelClient tunnelClient;
    StreamObserver<SignalRpcMessage> rpcStream;

    public GrpcWebSocket (WebSocketListener listener) throws IOException {
        GrpcConfig config = new GrpcConfig();
        config.target("localhost:50051");
        tunnelClient = new TunnelClient(config);
    }

    public void open(String destinationUri, Map<String, String> headers, WebSocketListener listener) {
        StreamListener<SignalRpcReply> streamListener = new StreamListener<>() {
            @Override
            public void onNext(SignalRpcReply v) {
                LOG.info("GRPCWS GOT MSG "+v.getMessage()+" with statuscode = "+v.getStatuscode());
                LOG.info("GRPCWS GOT MSG class = "+v.getMessage().getClass());
                LOG.info("GRPCWS length = "+v.getMessage().size());
                String sContent = new String(v.getMessage().toByteArray());
                LOG.info("GRPCWS GOT MSG str "+v.getMessage().toString());
                LOG.info("GRPCWS GOT MSG strc "+sContent);
                if ("111onOpen".equals(sContent)) {
                    LOG.info("that's an onopen msg");
                    listener.onOpen(GrpcWebSocket.this, null);
                    return;
                }
                if (v.getStatuscode() == -100) {
                  LOG.info("that's OUR onopen msg");
                    listener.onOpen(GrpcWebSocket.this, null);
                    return;  
                }
                ByteBuffer bbuff = v.getMessage().asReadOnlyByteBuffer();
                ByteString bb = ByteString.of(bbuff);
                listener.onMessage(GrpcWebSocket.this, bb);
            }
        };
        rpcStream = tunnelClient.openStream(destinationUri, headers, streamListener);

    }
    @Override
    public boolean close(int code, String reason) {
        LOG.severe("We are asked to close this websocket, reason = "+reason+" and code = "+code);
        return true;
    }

    @Override
    public boolean send(ByteString obs) {
        ByteBuffer bb = obs.asByteBuffer();
       
        SignalRpcMessage srm = SignalRpcMessage.newBuilder()
                .setBody(com.google.protobuf.ByteString.copyFrom(bb)).build();
        rpcStream.onNext(srm);
        return true;
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