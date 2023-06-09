package com.gluonhq.snl;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayOutputStream;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.internal.configuration.SignalUrl;
import org.whispersystems.signalservice.internal.push.SendGroupMessageResponse;
import org.whispersystems.signalservice.internal.websocket.WebSocketProtos;
import org.whispersystems.signalservice.internal.websocket.WebSocketProtos.WebSocketMessage;
import org.whispersystems.signalservice.internal.websocket.WebSocketProtos.WebSocketRequestMessage;

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
    private BlockingQueue<byte[]> rawByteQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<WebSocketRequestMessage> wsRequestMessageQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<SignalServiceEnvelope> envelopeQueue = new LinkedBlockingQueue<>();
    private Thread formatProcessingThread;
    private boolean closed = false;
    private static final String SERVER_DELIVERED_TIMESTAMP_HEADER = "X-Signal-Timestamp";

    public NetworkClient(SignalUrl url, String agent, boolean allowStories) {
        this(url, Optional.empty(), agent, allowStories);
    }

    public NetworkClient(SignalUrl url, Optional<CredentialsProvider> cp, String signalAgent, boolean allowStories) {
        this.signalUrl = url;
        this.signalAgent = signalAgent;
        this.allowStories = allowStories;
        this.httpClient = buildClient();
        this.credentialsProvider = cp;
        this.formatProcessingThread = new Thread() {
            @Override
            public void run() {
                processFormatConversion();
            }
        };
        this.formatProcessingThread.start();
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
                .replace("http://", "ws://") + "/v1/websocket/";
        if (this.credentialsProvider.isPresent()) {
            CredentialsProvider cp = this.credentialsProvider.get();
            String identifier = cp.getAci() != null ? cp.getDeviceUuid() : cp.getE164();
            baseUrl = baseUrl + "?login=" + identifier + "&password=" + cp.getPassword();
        }
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

    public void shutdown() {
        this.closed = true;
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Future<SendGroupMessageResponse> sendToGroup(byte[] ciphertext, byte[] joinedUnidentifiedAccess, long timestamp, boolean online) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isConnected() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public SignalServiceEnvelope read(long timeout, TimeUnit unit) {
        if (this.webSocket == null) { // TODO: create WS before starting to read
            try {
                createWebSocket();
            } catch (IOException ex) {
                Logger.getLogger(NetworkClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            while (true) { // we only return existing envelopes
                LOG.info("Wait for requestMessage...");
                WebSocketRequestMessage request = wsRequestMessageQueue.poll(timeout, unit);
                LOG.info("Got requestMessage, process now " + request);
                Optional<SignalServiceEnvelope> sse = handleWebSocketRequestMessage(request);
                if (sse.isPresent()) {
                    return sse.get();
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(NetworkClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    Optional<SignalServiceEnvelope> handleWebSocketRequestMessage(WebSocketRequestMessage request) throws IOException {
        WebSocketProtos.WebSocketResponseMessage response = createWebSocketResponse(request);

        try {
            if (isSignalServiceEnvelope(request)) {
                Optional<String> timestampHeader = findHeader(request, SERVER_DELIVERED_TIMESTAMP_HEADER);
                long timestamp = 0;

                if (timestampHeader.isPresent()) {
                    try {
                        timestamp = Long.parseLong(timestampHeader.get());
                    } catch (NumberFormatException e) {
                        LOG.warning("Failed to parse " + SERVER_DELIVERED_TIMESTAMP_HEADER);
                    }
                }

                SignalServiceEnvelope envelope = new SignalServiceEnvelope(request.getBody().toByteArray(), timestamp);
                LOG.finer("Request " + Objects.hashCode(request) + " has envelope " + Objects.hashCode(envelope));
                return Optional.of(envelope);
            } else if (isSocketEmptyRequest(request)) {
                return Optional.empty();
            }
        } finally {
            LOG.finer("[SSMP] readOrEmpty SHOULD send response");
            try {
                WebSocketMessage msg = WebSocketMessage.newBuilder()
                        .setType(WebSocketMessage.Type.RESPONSE)
                        .setResponse(response)
                        .build();
                msg.toByteArray();
                webSocket.sendBinary(ByteBuffer.wrap(msg.toByteArray()), true);
            } catch (Exception ioe) {
                LOG.log(Level.SEVERE, "IO exception in sending response", ioe);
            }
            LOG.fine("[SSMP] readOrEmpty did send response");
        }
        return Optional.empty();
    }

    private boolean isSignalServiceEnvelope(WebSocketRequestMessage message) {
        return "PUT".equals(message.getVerb()) && "/api/v1/message".equals(message.getPath());
    }

    private boolean isSocketEmptyRequest(WebSocketRequestMessage message) {
        return "PUT".equals(message.getVerb()) && "/api/v1/queue/empty".equals(message.getPath());
    }

    private WebSocketProtos.WebSocketResponseMessage createWebSocketResponse(WebSocketRequestMessage request) {
        if (isSignalServiceEnvelope(request)) {
            return WebSocketProtos.WebSocketResponseMessage.newBuilder()
                    .setId(request.getId())
                    .setStatus(200)
                    .setMessage("OK")
                    .build();
        } else {
            return WebSocketProtos.WebSocketResponseMessage.newBuilder()
                    .setId(request.getId())
                    .setStatus(400)
                    .setMessage("Unknown")
                    .build();
        }
    }

    private static Optional<String> findHeader(WebSocketRequestMessage message, String targetHeader) {
        if (message.getHeadersCount() == 0) {
            return Optional.empty();
        }

        for (String header : message.getHeadersList()) {
            if (header.startsWith(targetHeader)) {
                String[] split = header.split(":");
                if (split.length == 2 && split[0].trim().toLowerCase().equals(targetHeader.toLowerCase())) {
                    return Optional.of(split[1].trim());
                }
            }
        }

        return Optional.empty();
    }

    private void processFormatConversion() {
        LOG.info("start processformatthread");
        while (!closed) {
            try {
                LOG.info("Wait for raw bytes");
                byte[] raw = rawByteQueue.take();
                LOG.info("Got raw bytes");
                WebSocketMessage message = WebSocketMessage.parseFrom(raw);
                LOG.info("Got message, type = " + message.getType());
                if (message.getType() == WebSocketMessage.Type.REQUEST) {
                    LOG.info("Add request message to queue");
                    wsRequestMessageQueue.put(message.getRequest());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

        }
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

    class MyWebsocketListener implements WebSocket.Listener {

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOG.log(Level.SEVERE, "ERROR IN WEBSOCKET!", error);
            error.printStackTrace();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            Thread.dumpStack();
            throw new UnsupportedOperationException("Not supported yet.");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            LOG.info("Websocket receives binary data on " + Thread.currentThread() + ", last = " + last + ", limit = " + data.limit() + ", remaining = " + data.remaining() + ", cap = " + data.capacity());
            webSocket.request(1);
            byte[] buff = new byte[data.remaining()];
            data.get(buff);
            try {
                baos.write(buff);
                if (last) {
                    byte[] completed = baos.toByteArray();
                    baos = new ByteArrayOutputStream();
                    System.err.println("total size = " + completed.length);
                    rawByteQueue.put(completed);
                    //     listener.onMessage(answer, ByteString.of(completed));
                }
            } catch (Throwable t) {
                t.printStackTrace();
                LOG.log(Level.SEVERE, "error in receiving ws data", t);
            }
            return null;
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            LOG.info("Websocket receives text");
            webSocket.request(1);

            try {
                rawByteQueue.put(data.toString().getBytes());
                //   listener.onMessage(answer, data.toString());
            } catch (Throwable t) {
                t.printStackTrace();
                LOG.log(Level.SEVERE, "error in receiving ws data", t);

            }
            return null;

        }

        @Override
        public void onOpen(WebSocket webSocket) {
            try {
                LOG.info("java.net ws is opened");
                //     listener.onOpen(answer, null);
                LOG.info("notified listener1");
                java.net.http.WebSocket.Listener.super.onOpen(webSocket);
                System.err.println("notified listener2");
            } catch (Throwable e) {
                e.printStackTrace();
                LOG.log(Level.SEVERE, "error in onopen", e);
            }

        }
    }
}
