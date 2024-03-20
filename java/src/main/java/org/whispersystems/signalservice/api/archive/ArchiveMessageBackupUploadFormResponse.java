package org.whispersystems.signalservice.api.archive;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 *
 * @author johan
 */
public class ArchiveMessageBackupUploadFormResponse{
        @JsonProperty
        int cdn;
        @JsonProperty
        String key;
        @JsonProperty
        Map<String, String> headers;
        @JsonProperty
        String signedUploadLocation; 

        public int getCdn() {
            return this.cdn;
        }
        
        public Map<String, String> getHeaders() {
            return this.headers;
        }
        
        public String getSignedUploadLocation() {
            return this.signedUploadLocation;
        }
}
