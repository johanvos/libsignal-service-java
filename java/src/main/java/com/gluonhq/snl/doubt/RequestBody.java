package com.gluonhq.snl.doubt;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author johan
 */
public class RequestBody {

    MediaType mediaType;
    String jsonBody;
    
    public RequestBody() {
    }
    
    RequestBody(MediaType mt, String jb) {
        this.mediaType = mt;
        this.jsonBody = jb;
    }
    public static RequestBody create(MediaType mt, String jsonBody) {
        return new RequestBody(mt, jsonBody);
    }

    public long contentLength() {
        throw new UnsupportedOperationException("NYI");
    }

    public MediaType contentType() {
        return this.mediaType;
    }

    public byte[] getRawBytes() {
        return jsonBody.getBytes();
    }

    public void writeTo(OutputStream sink) throws IOException {
        throw new UnsupportedOperationException("NYI");
    }
}
