package org.whispersystems.signalservice.internal.push;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 *
 * @author johan
 */
public class AttachmentUploadForm {
    @JsonProperty
    int cdn;

    @JsonProperty
    String key;

    @JsonProperty
    Map<String, String> headers;

    @JsonProperty
    String signedUploadLocation;
}
