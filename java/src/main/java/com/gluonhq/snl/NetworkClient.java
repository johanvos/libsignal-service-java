package com.gluonhq.snl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.internal.configuration.SignalUrl;
import org.whispersystems.signalservice.internal.push.SendGroupMessageResponse;

/**
 *
 * @author johan
 */
public class NetworkClient {

    final HttpClient httpClient;
    final SignalUrl signalUrl;
    final String signalAgent;
    final boolean allowStories;
    final Optional<CredentialsProvider> credentialsProvider;
    private static final Logger LOG = Logger.getLogger(NetworkClient.class.getName());
    private WebSocket webSocket;

    public NetworkClient(SignalUrl url, String agent, boolean allowStories) {
        this(url, Optional.empty(), agent, allowStories);
    }

    public NetworkClient(SignalUrl url, Optional<CredentialsProvider> cp, String signalAgent, boolean allowStories) {
        this.signalUrl = url;
        this.signalAgent = signalAgent;
        this.allowStories = allowStories;
        this.httpClient = buildClient();
        this.credentialsProvider = cp;
    }

    private HttpClient buildClient() {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        HttpClient answer = clientBuilder.build();
        return answer;
    }

    private void createWebSocket() throws IOException {
        WebSocket.Builder wsBuilder = this.httpClient.newWebSocketBuilder();
        wsBuilder.header("X-Signal-Agent", signalAgent);
        wsBuilder.header("X-Signal-Receive-Stories", allowStories ? "true" : "false");
        String baseUrl = signalUrl.getUrl().replace("https://", "wss://")
                .replace("http://", "ws://");
        if (this.credentialsProvider.isPresent()) {
            CredentialsProvider cp = this.credentialsProvider.get();
            String identifier = cp.getAci() != null ? cp.getDeviceUuid() : cp.getE164();
            baseUrl = baseUrl + "?login=" + identifier + "&password=" + cp.getPassword();
        }
        URI uri = null;
        try {
            uri = new URI(baseUrl);
        } catch (URISyntaxException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new IOException("Can not create websocket to wrong formatted url", ex);
        }
        if (uri == null) {
            throw new IOException("Can not create websocket to unexisting url");
        }
        CompletableFuture<WebSocket> webSocketProcess = wsBuilder.buildAsync(uri, new WebSocket.Listener() {
            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void onOpen(WebSocket webSocket) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            
        });
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
        if (this.webSocket == null) throw new IOException ("Could not create a websocket");

    }

    public void shutdown() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Future<SendGroupMessageResponse> sendToGroup(byte[] ciphertext, byte[] joinedUnidentifiedAccess, long timestamp, boolean online) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isConnected() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SignalServiceEnvelope read(long timeout, TimeUnit unit) {
        if (this.webSocket == null) {
            try {
                createWebSocket();
            } catch (IOException ex) {
                Logger.getLogger(NetworkClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(NetworkClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    HttpResponse.BodyHandler createBodyHandler() {
        HttpResponse.BodyHandler mbh = new HttpResponse.BodyHandler() {
            @Override
            public HttpResponse.BodySubscriber apply(HttpResponse.ResponseInfo responseInfo) {
                String ct = responseInfo.headers().firstValue("content-type").orElse("");
                LOG.info("response statuscode = " + responseInfo.statusCode() + ", content-type = " + ct);
                if (responseInfo.statusCode() == 428) {
                    LOG.info("Got 428 response! all headers = " + responseInfo.headers().map());
                }
                if (ct.isBlank()) {
                    return BodySubscribers.discarding();
                }
                if ((ct.equals("application/json") || (ct.equals("application/xml")))) {
                    return BodySubscribers.ofString(StandardCharsets.UTF_8);
                } else {
                    return BodySubscribers.ofByteArray();
                }
            }
        };
        return mbh;
    }

    public HttpResponse sendRequest(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse response = this.httpClient.send(request, createBodyHandler());
        return response;
    }

}
