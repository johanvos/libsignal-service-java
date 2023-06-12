package com.gluonhq.snl;

import java.net.http.HttpResponse;
import java.util.List;

/**
 *
 * @author johan
 */
public class Response<T> {

    private HttpResponse<T> httpAnswer;
    T body;

    public Response(byte[] rawBytes) {
        this.body = (T)rawBytes;
    }
    public Response(HttpResponse<T> httpAnswer) {
        this.httpAnswer = httpAnswer;
        this.body = httpAnswer.body();
        System.err.println("Got response "+httpAnswer.body());
    }

    public ResponseBody<T> body() {
        return new ResponseBody(body);
    }

    public List<String> headers(String setCookie) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String header(String key) {
                throw new UnsupportedOperationException("Not supported yet.");

    }
    public boolean isSuccessful() {
        int sc = httpAnswer.statusCode();
        return ((sc >= 200) && (sc < 300));
    }

    public int getStatusCode() {
        return httpAnswer.statusCode();
    }

    public String message() {
        return "RENAME ME TO GETMESSAGE!";
    }

}
