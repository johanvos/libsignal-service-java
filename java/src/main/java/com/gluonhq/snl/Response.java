package com.gluonhq.snl;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author johan
 */
public class Response<T> {

  //  private HttpResponse<T> httpAnswer;
    private final int statusCode;
    T body;
    Map<String, String> responseHeaders = new HashMap<>();

    public Response(byte[] rawBytes, int statusCode) {
        this.body = (T)rawBytes;
        this.statusCode = statusCode;
    }

    public Response(HttpResponse<T> httpAnswer) {
        this.body = httpAnswer.body();
        this.statusCode = httpAnswer.statusCode();
        HttpHeaders headers = httpAnswer.headers();
        headers.map().keySet().stream().forEach(key -> headers.firstValue(key).ifPresent(val -> responseHeaders.put(key.toLowerCase(), val)));
    }

    public ResponseBody<T> body() {
        return new ResponseBody(body);
    }

    public List<String> headers(String setCookie) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String header(String key) {
        return responseHeaders.get(key.toLowerCase());        
    }

    public boolean isSuccessful() {
        return ((statusCode >= 200) && (statusCode < 300));
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String message() {
        return "RENAME ME TO GETMESSAGE!";
    }

}
