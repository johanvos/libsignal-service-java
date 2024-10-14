package org.whispersystems.signalservice.api.archive;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RemoteAttachment(@JsonProperty int cdn, @JsonProperty String key) {}
