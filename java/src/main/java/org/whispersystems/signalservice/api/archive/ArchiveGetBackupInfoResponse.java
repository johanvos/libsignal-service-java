package org.whispersystems.signalservice.api.archive;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author johan
 */
public class ArchiveGetBackupInfoResponse {

    @JsonProperty
    public int cdn;

    @JsonProperty
    public String mediaDir;

    @JsonProperty
    public String backupDir;

    @JsonProperty
    public String backupName;

    @JsonProperty
    long usedSpace;
}
