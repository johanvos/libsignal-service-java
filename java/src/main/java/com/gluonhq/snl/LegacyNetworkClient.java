package com.gluonhq.snl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;
import org.whispersystems.signalservice.internal.configuration.SignalUrl;

/**
 *
 * @author johan
 */
public class LegacyNetworkClient extends NetworkClient {

    private static final Logger LOG = Logger.getLogger(LegacyNetworkClient.class.getName());
    private WebSocket webSocket;

    public LegacyNetworkClient(SignalUrl url, Optional<CredentialsProvider> cp, String signalAgent, Optional<ConnectivityListener> connectivityListener, boolean allowStories) {
        super(url, cp, signalAgent, connectivityListener, allowStories);
    }

    void implCreateWebSocket(String baseUrl) throws IOException {
        WebSocket.Builder wsBuilder = this.httpClient.newWebSocketBuilder();
        wsBuilder.header("X-Signal-Agent", signalAgent);
        wsBuilder.header("X-Signal-Receive-Stories", allowStories ? "true" : "false");
        URI uri = null;
        try {
            LOG.info("CREATEWS to " + baseUrl);
            uri = new URI(baseUrl);
        } catch (URISyntaxException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new IOException("Can not create websocket to wrong formatted url", ex);
        }
        if (uri == null) {
            throw new IOException("Can not create websocket to unexisting url");
        }
        WebSocket.Listener myListener = new MyWebsocketListener();
        CompletableFuture<WebSocket> webSocketProcess = wsBuilder.buildAsync(uri, myListener);

        CountDownLatch cdl = new CountDownLatch(1);
        Executors.newCachedThreadPool().submit(() -> {
            try {
                LOG.info("Joining ws...");
                this.webSocket = webSocketProcess.join();
                LOG.info("Done joining ws");
                cdl.countDown();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
        try {
            boolean res = cdl.await(10, TimeUnit.SECONDS);
            if (!res) {
                LOG.severe("Failed to reconnect!");
            }
        } catch (InterruptedException ex) {
            LOG.warning("Interrupted while waiting for websocket connection");
            LOG.log(Level.SEVERE, null, ex);
        }
        if (this.webSocket == null) {
            throw new IOException("Could not create a websocket");
        }

    }

    void sendToStream(byte[] payload) throws IOException {
        this.webSocket.sendBinary(ByteBuffer.wrap(payload), true);
    }

    void implShutdown() {
        if (this.webSocket != null) {
            this.webSocket.abort();
        }
    }

}
