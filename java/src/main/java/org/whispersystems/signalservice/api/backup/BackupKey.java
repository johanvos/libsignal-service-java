package org.whispersystems.signalservice.api.backup;

/**
 *
 * @author johan
 */
public class BackupKey {

    private final byte[] value;

    public BackupKey(byte[] val) {
        if (val.length != 32) throw new IllegalArgumentException ("BackupKey should be 32 bytes");
        this.value = val;
    }

    public byte[] serialize() {
        return this.value;
    }

}
