package com.gluonhq.snl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;

/**
 *
 * @author johan
 */
public class NetworkClient {
    
    final HttpClient httpClient;
    
    public NetworkClient() {
        this.httpClient = buildClient();
    }

    private HttpClient buildClient() {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        HttpClient answer = clientBuilder.build();
        return answer;
    }

    private void createWebSocket(URI uri) {
        WebSocket.Builder wsBuilder = this.httpClient.newWebSocketBuilder();
        wsBuilder.buildAsync(uri, new WebSocket.Listener() {
        });
    }
}
