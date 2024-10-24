package org.whispersystems.signalservice.api.archive;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CopyMediaRequest(
        @JsonProperty RemoteAttachment sourceAttachment, 
        @JsonProperty int objectLength, 
        @JsonProperty byte[] mediaId, 
        @JsonProperty byte[] hmacKey, 
        @JsonProperty byte[] encryptionKey, 
        @JsonProperty byte[] iv) {}
