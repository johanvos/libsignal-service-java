package org.whispersystems.signalservice.internal.websocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebsocketResponse {

    private final int status;
    private final String body;
    private final Map<String, String> headers;

    WebsocketResponse(int status, String body, List<String> headers) {
        this.status = status;
        this.body = body;
        this.headers = parseHeaders(headers);

    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    public String getHeader(String key) {
        return headers.get(key.toLowerCase());
    }

    private static Map<String, String> parseHeaders(List<String> rawHeaders) {
        Map<String, String> headers = new HashMap<>(rawHeaders.size());

        for (String raw : rawHeaders) {
            if (raw != null && raw.length() > 0) {
                int colonIndex = raw.indexOf(":");

                if (colonIndex > 0 && colonIndex < raw.length() - 1) {
                    String key = raw.substring(0, colonIndex).trim().toLowerCase();
                    String value = raw.substring(colonIndex + 1).trim();

                    headers.put(key, value);
                }
            }
        }
        return headers;
    }

}
