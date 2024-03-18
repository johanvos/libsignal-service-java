package org.whispersystems.signalservice.api.kbs;

import org.whispersystems.signalservice.api.storage.StorageKey;
import org.whispersystems.signalservice.internal.util.Hex;
import org.whispersystems.util.StringUtil;

import java.security.SecureRandom;
import java.util.Arrays;
import org.signal.libsignal.protocol.kdf.HKDF;
import org.whispersystems.signalservice.api.backup.BackupKey;

import static org.whispersystems.signalservice.api.crypto.CryptoUtil.hmacSha256;

public final class MasterKey {

  private static final int LENGTH = 32;

  private final byte[] masterKey;

  public MasterKey(byte[] masterKey) {
    if (masterKey.length != LENGTH) throw new AssertionError();

    this.masterKey = masterKey;
  }

  public static MasterKey createNew(SecureRandom secureRandom) {
    byte[] key = new byte[LENGTH];
    secureRandom.nextBytes(key);
    return new MasterKey(key);
  }

  public String deriveRegistrationLock() {
    return Hex.toStringCondensed(derive("Registration Lock"));
  }

  public StorageKey deriveStorageServiceKey() {
    return new StorageKey(derive("Storage Service Encryption"));
  }

  public BackupKey deriveBackupKey() {
    return new BackupKey(HKDF.deriveSecrets(masterKey, "20231003_Signal_Backups_GenerateBackupKey".getBytes(), 32));
  }

  private byte[] derive(String keyName) {
    return hmacSha256(masterKey, StringUtil.utf8(keyName));
  }

  public byte[] serialize() {
    return masterKey.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || o.getClass() != getClass()) return false;

    return Arrays.equals(((MasterKey) o).masterKey, masterKey);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(masterKey);
  }
}
