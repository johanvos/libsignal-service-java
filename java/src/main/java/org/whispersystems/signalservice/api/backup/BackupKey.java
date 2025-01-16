package org.whispersystems.signalservice.api.backup;

import java.util.Arrays;
import org.signal.libsignal.protocol.kdf.HKDF;
import org.whispersystems.signalservice.api.push.ServiceId.ACI;

/**
 *
 * @author johan
 */
public class BackupKey {

    private final byte[] backupKeyBytes;

    final static String BACKUP_KEY_INFO = "20231003_Signal_Backups_GenerateBackupId";
    final static String MESSAGE_KEY_INFO = "20231003_Signal_Backups_EncryptMessageBackup";

    final static int BACKUP_ID_LENGTH = 16;
    final static int BACKUP_KEY_LENGTH = 80;
    final static int MEDIA_ID_LENGTH = 15;
    final static String MEDIA_ID_INFO = "Media ID";
    final static int MEDIA_KEY_LENGTH = 80;
    final static String MEDIA_KEY_INFO = "20231003_Signal_Backups_EncryptMedia";

    public BackupKey(byte[] val) {
        if (val.length != 32) throw new IllegalArgumentException ("BackupKey should be 32 bytes");
        this.backupKeyBytes = val;
    }

    public byte[] serialize() {
        return this.backupKeyBytes;
    }

    public KeyMaterial derviceSecrets(ACI aci) {
        byte[] salt = aci.toByteArray();
        byte[] backupId = HKDF.deriveSecrets(backupKeyBytes, salt, BACKUP_KEY_INFO.getBytes(), BACKUP_ID_LENGTH);
        byte[] extKey = HKDF.deriveSecrets(backupKeyBytes, backupId, MESSAGE_KEY_INFO.getBytes(),  BACKUP_KEY_LENGTH);
        return new KeyMaterial(backupId,
                Arrays.copyOfRange(extKey, 0, 32),
                Arrays.copyOfRange(extKey, 32, 64),
                Arrays.copyOfRange(extKey, 64, 80));
    }

    public KeyMaterial deriveMediaSecrets(byte[] dataHash) {
        byte[] mediaId = deriveMediaId(dataHash);
        byte[] extKey = HKDF.deriveSecrets(backupKeyBytes, mediaId, MEDIA_KEY_INFO.getBytes(), MEDIA_KEY_LENGTH);
        return new KeyMaterial(mediaId,
                Arrays.copyOfRange(extKey, 0, 32),
                Arrays.copyOfRange(extKey, 32, 64),
                Arrays.copyOfRange(extKey, 64, 80));
    }

    byte[] deriveMediaId(byte[] dataHash) {
        return HKDF.deriveSecrets(backupKeyBytes, dataHash, MEDIA_ID_INFO.getBytes(), MEDIA_ID_LENGTH);
    }

    public record KeyMaterial(byte[] id, byte[] macKey, byte[] cipherKey, byte[] iv) {}
}
