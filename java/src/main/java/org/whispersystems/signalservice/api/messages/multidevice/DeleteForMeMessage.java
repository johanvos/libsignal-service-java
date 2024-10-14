package org.whispersystems.signalservice.api.messages.multidevice;

import java.util.List;

/**
 *
 * @author johan
 */
public class DeleteForMeMessage {
    
    private long sentTimestamp;
    private String authorAci;
    private String threadAci;
    private byte[] threadGroupId;
    private boolean fullDelete = false;
    private boolean hasMessageDelete = false;
    private boolean hasConversationDelete = false;
    private List<AddressableMessage> mostRecentMessages;
    
    protected DeleteForMeMessage() {}

    public DeleteForMeMessage(String threadAci, String authorAci, long timestamp) {
        this.authorAci = authorAci;
        this.sentTimestamp = timestamp;
        this.threadAci = threadAci;
        this.hasMessageDelete  = true;
    }
        
    public DeleteForMeMessage(byte[] threadGroupId, String authorAci, long timestamp) {
        this.authorAci = authorAci;
        this.sentTimestamp = timestamp;
        this.threadGroupId = threadGroupId;
        this.hasMessageDelete = true;
    }

    public static DeleteForMeMessage conversationDelete(String threadAci, List<AddressableMessage> mostRecentMessages) {
        DeleteForMeMessage answer = new DeleteForMeMessage();
        answer.hasConversationDelete = true;
        answer.fullDelete = true;
        answer.threadAci = threadAci;
        answer.mostRecentMessages = mostRecentMessages;
        return answer;
    }

    public static DeleteForMeMessage conversationDelete(byte[] threadGroupId, List<AddressableMessage> mostRecentMessages) {
        DeleteForMeMessage answer = new DeleteForMeMessage();
        answer.hasConversationDelete = true;
        answer.fullDelete = true;
        answer.threadGroupId = threadGroupId;
        answer.mostRecentMessages = mostRecentMessages;
        return answer; 
    }

    public boolean hasMessageDelete() {
        return this.hasMessageDelete;
    }

    public boolean hasConversationDelete() {
        return this.hasConversationDelete;
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

    public boolean isFullDelete() {
        return this.fullDelete;
    }

    public List<AddressableMessage> getMostRecentMessages() {
        return this.mostRecentMessages;
    }

    public static class AddressableMessage {
        long timestamp;
        String authorAci;
        
        public long getTimestamp() {
            return this.timestamp;
        }
        
        public String getAuthorAci() {
            return this.authorAci;
        }

        public AddressableMessage(String authorAci, long timestamp) {
            this.authorAci = authorAci;
            this.timestamp = timestamp;
        }
    }
}
