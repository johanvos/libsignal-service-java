package com.gluonhq.snl;

import java.net.http.HttpResponse;
import java.util.List;

/**
 *
 * @author johan
 */
public class Response<T> {

    private HttpResponse<T> httpAnswer;

    public Response(HttpResponse<T> httpAnswer) {
        this.httpAnswer = httpAnswer;
        System.err.println("Got response "+httpAnswer.body());
    }

    public ResponseBody<T> body() {
        T realBody = httpAnswer.body();
        return new ResponseBody(realBody);
    }

    public List<String> headers(String setCookie) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
