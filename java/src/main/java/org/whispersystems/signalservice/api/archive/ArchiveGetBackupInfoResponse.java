package org.whispersystems.signalservice.api.archive;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author johan
 */
public class ArchiveGetBackupInfoResponse {

    @JsonProperty
    int cdn;

    @JsonProperty
    String backupDir;

    @JsonProperty
    String backupName;

    @JsonProperty
    long usedSpace;
}
