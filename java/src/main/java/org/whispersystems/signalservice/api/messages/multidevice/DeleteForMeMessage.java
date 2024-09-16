package org.whispersystems.signalservice.api.messages.multidevice;

/**
 *
 * @author johan
 */
public class DeleteForMeMessage {
    
    private long sentTimestamp;
    private String authorAci;
    private String threadAci;
    private byte[] threadGroupId;
    
    public DeleteForMeMessage(String threadAci, String authorAci, long timestamp) {
        this.authorAci = authorAci;
        this.sentTimestamp = timestamp;
        this.threadAci = threadAci;
    }
        
    public DeleteForMeMessage(byte[] threadGroupId, String authorAci, long timestamp) {
        this.authorAci = authorAci;
        this.sentTimestamp = timestamp;
        this.threadGroupId = threadGroupId;
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

    public byte[] getThreadGroupId() {
        return this.threadGroupId;
    }

}
