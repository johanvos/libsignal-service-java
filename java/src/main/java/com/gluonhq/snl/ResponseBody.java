package com.gluonhq.snl;

/**
 *
 * @author johan
 */
public class ResponseBody<T> {

    T body;
    
    public ResponseBody(T t) {
        this.body = t;
    }

    public String string() {
        return (String)body;
    }

    public byte[] bytes() {
        if (body instanceof String bodyString) return bodyString.getBytes();
        if (body == null) return new byte[0];
        return (byte[]) body;
    }
    
}
