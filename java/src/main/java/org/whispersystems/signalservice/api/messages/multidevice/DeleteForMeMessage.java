package org.whispersystems.signalservice.api.messages.multidevice;

/**
 *
 * @author johan
 */
public class DeleteForMeMessage {
    
    private long sentTimestamp;
    private String authorAci;
    private String threadAci;
    
    public DeleteForMeMessage(String threadAci, String authorAci, long timestamp) {
        this.authorAci = authorAci;
        this.sentTimestamp = timestamp;
        this.threadAci = threadAci;
    }
    
    public String getAuthorAci() {
        return authorAci;
    }
    
    public long getSentTimestamp() {
        return sentTimestamp;
    }

    public String getThreadAci() {
        return this.threadAci;
    }
}
