package org.whispersystems.signalservice.api.backup;

import java.util.Arrays;
import org.signal.libsignal.protocol.kdf.HKDF;

/**
 *
 * @author johan
 */
public class BackupKey {

    private final byte[] keyBytes;

    final static int MEDIA_ID_LENGTH = 15;
    final static String MEDIA_ID_INFO = "Media ID";
    final static int MEDIA_KEY_LENGTH = 80;
    final static String MEDIA_KEY_INFO = "20231003_Signal_Backups_EncryptMedia";

    public BackupKey(byte[] val) {
        if (val.length != 32) throw new IllegalArgumentException ("BackupKey should be 32 bytes");
        this.keyBytes = val;
    }

    public byte[] serialize() {
        return this.keyBytes;
    }

    public KeyMaterial deriveMediaSecrets(byte[] dataHash) {
        byte[] mediaId = deriveMediaId(dataHash);
        byte[] extKey = HKDF.deriveSecrets(keyBytes, mediaId, MEDIA_KEY_INFO.getBytes(), MEDIA_KEY_LENGTH);
        return new KeyMaterial(mediaId,
                Arrays.copyOfRange(extKey, 0, 32),
                Arrays.copyOfRange(extKey, 32, 64),
                Arrays.copyOfRange(extKey, 64, 80));
    }

    byte[] deriveMediaId(byte[] dataHash) {
        return HKDF.deriveSecrets(keyBytes, dataHash, MEDIA_ID_INFO.getBytes(), MEDIA_ID_LENGTH);
    }

    public record KeyMaterial(byte[] id, byte[] macKey, byte[] cipherKey, byte[] iv) {}
}
