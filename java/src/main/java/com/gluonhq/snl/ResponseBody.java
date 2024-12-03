package com.gluonhq.snl;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 *
 * @author johan
 */
public class ResponseBody<T> {

    T body;
    
    private static final Logger LOG = Logger.getLogger(ResponseBody.class.getName());

    public ResponseBody(T t) {
        this.body = t;
       // Thread.dumpStack();
        LOG.info("Created Responsebody with type "+body.getClass()+" and content = "+body);
        if (body instanceof String bodyString) {
            LOG.info("And bodybytes = "+Arrays.toString(bodyString.getBytes()));
        }
    }

    public String string() {
        LOG.info("RESPONSEBODY, class = "+body.getClass());
        if (body instanceof String bs) {
            LOG.info("will return this string: "+bs+" with bytes "+Arrays.toString(bs.getBytes()));
            return bs;
        }
        if (body instanceof byte[] rb) return new String(rb);
        throw new IllegalArgumentException ("Can't convert "+body+" to string");
    }

    public byte[] bytes() {
        LOG.info("RESPONSEBODY, class = "+ body.getClass());
        if (body instanceof String bodyString) return bodyString.getBytes();
        if (body == null) return new byte[0];
        return (byte[]) body;
    }

    public int contentLength() {
        if (body instanceof byte[] bb) return bb.length;
        return -1;
    }

    public void close(){
    }
}
