/*
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.crypto;

import com.google.protobuf.InvalidProtocolBufferException;

import org.signal.libsignal.metadata.InvalidMetadataMessageException;
import org.signal.libsignal.metadata.InvalidMetadataVersionException;
import org.signal.libsignal.metadata.ProtocolDuplicateMessageException;
import org.signal.libsignal.metadata.ProtocolInvalidKeyException;
import org.signal.libsignal.metadata.ProtocolInvalidKeyIdException;
import org.signal.libsignal.metadata.ProtocolInvalidMessageException;
import org.signal.libsignal.metadata.ProtocolInvalidVersionException;
import org.signal.libsignal.metadata.ProtocolLegacyMessageException;
import org.signal.libsignal.metadata.ProtocolNoSessionException;
import org.signal.libsignal.metadata.ProtocolUntrustedIdentityException;
import org.signal.libsignal.metadata.SealedSessionCipher;
import org.signal.libsignal.metadata.SealedSessionCipher.DecryptionResult;
import org.signal.libsignal.metadata.SelfSendException;
import org.signal.libsignal.metadata.certificate.CertificateValidator;
import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.signalservice.api.SignalSessionLock;
import org.whispersystems.signalservice.api.messages.SignalServiceContent;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.api.messages.SignalServiceMetadata;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.util.UuidUtil;
import org.whispersystems.signalservice.internal.push.OutgoingPushMessage;
import org.whispersystems.signalservice.internal.push.PushTransportDetails;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.Envelope.Type;
import org.whispersystems.signalservice.internal.push.UnsupportedDataMessageException;
import org.whispersystems.signalservice.internal.serialize.SignalServiceAddressProtobufSerializer;
import org.whispersystems.signalservice.internal.serialize.SignalServiceMetadataProtobufSerializer;
import org.whispersystems.signalservice.internal.serialize.protos.SignalServiceContentProto;
import org.whispersystems.util.Base64;

import java.util.Optional;
import org.whispersystems.signalservice.api.InvalidMessageStructureException;

/**
 * This is used to decrypt received {@link SignalServiceEnvelope}s.
 *
 * @author Moxie Marlinspike
 */
public class SignalServiceCipher {

  @SuppressWarnings("unused")
  private static final String TAG = SignalServiceCipher.class.getSimpleName();

  private final SignalProtocolStore  signalProtocolStore;
  private final SignalSessionLock    sessionLock;
  private final SignalServiceAddress localAddress;
  private final CertificateValidator certificateValidator;

  public SignalServiceCipher(SignalServiceAddress localAddress,
                             SignalProtocolStore signalProtocolStore,
                             SignalSessionLock sessionLock,
                             CertificateValidator certificateValidator)
  {
    this.signalProtocolStore  = signalProtocolStore;
    this.sessionLock          = sessionLock;
    this.localAddress         = localAddress;
    this.certificateValidator = certificateValidator;
  }
  public OutgoingPushMessage encrypt(SignalProtocolAddress        destination,
                                     Optional<UnidentifiedAccess> unidentifiedAccess,
                                     EnvelopeContent              content)
      throws UntrustedIdentityException, InvalidKeyException
  {
    if (unidentifiedAccess.isPresent()) {
      SignalSessionCipher       sessionCipher        = new SignalSessionCipher(sessionLock, new SessionCipher(signalProtocolStore, destination));
      SignalSealedSessionCipher sealedSessionCipher  = 
              new SignalSealedSessionCipher(sessionLock, new SealedSessionCipher(signalProtocolStore, localAddress.getUuid().orElse(null), localAddress.getNumber().orElse(null), 1));

      return content.processSealedSender(sessionCipher, sealedSessionCipher, destination, unidentifiedAccess.get().getUnidentifiedCertificate());
    } else {
      SignalSessionCipher sessionCipher = new SignalSessionCipher(sessionLock, new SessionCipher(signalProtocolStore, destination));

      return content.processUnsealedSender(sessionCipher, destination);
    }
  }

  public OutgoingPushMessage encrypt(SignalProtocolAddress        destination,
                                     Optional<UnidentifiedAccess> unidentifiedAccess,
                                     byte[]                       unpaddedMessage)
      throws UntrustedIdentityException, InvalidKeyException
  {
    if (unidentifiedAccess.isPresent()) {
      SignalSealedSessionCipher sessionCipher        = new SignalSealedSessionCipher(sessionLock, new SealedSessionCipher(signalProtocolStore, localAddress.getUuid().orElse(null), localAddress.getNumber().orElse(null), 1));
      PushTransportDetails      transportDetails     = new PushTransportDetails(sessionCipher.getSessionVersion(destination));
      byte[]                    ciphertext           = sessionCipher.encrypt(destination, unidentifiedAccess.get().getUnidentifiedCertificate(), transportDetails.getPaddedMessageBody(unpaddedMessage));
      String                    body                 = Base64.encodeBytes(ciphertext);
      int                       remoteRegistrationId = sessionCipher.getRemoteRegistrationId(destination);

      return new OutgoingPushMessage(Type.UNIDENTIFIED_SENDER_VALUE, destination.getDeviceId(), remoteRegistrationId, body);
    } else {
      SignalSessionCipher  sessionCipher        = new SignalSessionCipher(sessionLock, new SessionCipher(signalProtocolStore, destination));
      PushTransportDetails transportDetails     = new PushTransportDetails(sessionCipher.getSessionVersion());
      CiphertextMessage    message              = sessionCipher.encrypt(transportDetails.getPaddedMessageBody(unpaddedMessage));
      int                  remoteRegistrationId = sessionCipher.getRemoteRegistrationId();
      String               body                 = Base64.encodeBytes(message.serialize());

      int type;

      switch (message.getType()) {
        case CiphertextMessage.PREKEY_TYPE:  type = Type.PREKEY_BUNDLE_VALUE; break;
        case CiphertextMessage.WHISPER_TYPE: type = Type.CIPHERTEXT_VALUE;    break;
        default: throw new AssertionError("Bad type: " + message.getType());
      }

      return new OutgoingPushMessage(type, destination.getDeviceId(), remoteRegistrationId, body);
    }
  }

  /**
   * Decrypt a received {@link SignalServiceEnvelope}
   *
   * @param envelope The received SignalServiceEnvelope
   *
   * @return a decrypted SignalServiceContent
   */
  public SignalServiceContent decrypt(SignalServiceEnvelope envelope)
      throws InvalidMetadataMessageException, InvalidMetadataVersionException,
      ProtocolInvalidKeyIdException, ProtocolLegacyMessageException,
      ProtocolUntrustedIdentityException, ProtocolNoSessionException,
      ProtocolInvalidVersionException, ProtocolInvalidMessageException,
      ProtocolInvalidKeyException, ProtocolDuplicateMessageException,
      SelfSendException, UnsupportedDataMessageException
  {
    try {
      if (envelope.hasLegacyMessage()) {
        Plaintext                       plaintext   = decrypt(envelope, envelope.getLegacyMessage());
        SignalServiceProtos.DataMessage dataMessage = SignalServiceProtos.DataMessage.parseFrom(plaintext.getData());

        SignalServiceContentProto contentProto = SignalServiceContentProto.newBuilder()
                                                                          .setLocalAddress(SignalServiceAddressProtobufSerializer.toProtobuf(localAddress))
                                                                          .setMetadata(SignalServiceMetadataProtobufSerializer.toProtobuf(plaintext.metadata))
                                                                          .setLegacyDataMessage(dataMessage)
                                                                          .build();

        return SignalServiceContent.createFromProto(contentProto);
      } else if (envelope.hasContent()) {
          System.err.println("SSC will decrypt envelope with type " + envelope.getType()+
                  " and sourceguid = "+envelope.getSourceUuid().orElse("unknown"));
        Plaintext                   plaintext = decrypt(envelope, envelope.getContent());
          System.err.println("PLAINTEXT = "+plaintext);
        SignalServiceProtos.Content content   = SignalServiceProtos.Content.parseFrom(plaintext.getData());
          System.err.println("CONTENT = "+content);
        SignalServiceContentProto contentProto = SignalServiceContentProto.newBuilder()
                                                                          .setLocalAddress(SignalServiceAddressProtobufSerializer.toProtobuf(localAddress))
                                                                          .setMetadata(SignalServiceMetadataProtobufSerializer.toProtobuf(plaintext.metadata))
                                                                          .setContent(content)
                                                                          .build();
        return SignalServiceContent.createFromProto(contentProto);
      }

      return null;
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidMetadataMessageException(e);
    } catch (InvalidMessageStructureException e2) {
      throw new InvalidMetadataMessageException(e2);
    }
  }

  private Plaintext decrypt(SignalServiceEnvelope envelope, byte[] ciphertext)
      throws InvalidMetadataMessageException, InvalidMetadataVersionException,
      ProtocolDuplicateMessageException, ProtocolUntrustedIdentityException,
      ProtocolLegacyMessageException, ProtocolInvalidKeyException,
      ProtocolInvalidVersionException, ProtocolInvalidMessageException,
      ProtocolInvalidKeyIdException, ProtocolNoSessionException,
      SelfSendException
  {
    try {

      byte[]                paddedMessage;
      SignalServiceMetadata metadata;
      int                   sessionVersion;

      if (!envelope.hasSource() && !envelope.isUnidentifiedSender()) {
        throw new ProtocolInvalidMessageException(new InvalidMessageException("Non-UD envelope is missing a source!"), null, 0);
      }

      if (envelope.isPreKeySignalMessage()) {
        SignalProtocolAddress sourceAddress = getPreferredProtocolAddress(signalProtocolStore, envelope.getSourceAddress(), envelope.getSourceDevice());
        SignalSessionCipher   sessionCipher = new SignalSessionCipher(sessionLock, new SessionCipher(signalProtocolStore, sourceAddress));

        paddedMessage  = sessionCipher.decrypt(new PreKeySignalMessage(ciphertext));
        metadata       = new SignalServiceMetadata(envelope.getSourceAddress(), envelope.getSourceDevice(), envelope.getTimestamp(), envelope.getServerReceivedTimestamp(), envelope.getServerDeliveredTimestamp(), false);
        sessionVersion = sessionCipher.getSessionVersion();
      } else if (envelope.isSignalMessage()) {
        SignalProtocolAddress sourceAddress = getPreferredProtocolAddress(signalProtocolStore, envelope.getSourceAddress(), envelope.getSourceDevice());
        SignalSessionCipher   sessionCipher = new SignalSessionCipher(sessionLock, new SessionCipher(signalProtocolStore, sourceAddress));

        paddedMessage  = sessionCipher.decrypt(new SignalMessage(ciphertext));
        metadata       = new SignalServiceMetadata(envelope.getSourceAddress(), envelope.getSourceDevice(), envelope.getTimestamp(), envelope.getServerReceivedTimestamp(), envelope.getServerDeliveredTimestamp(), false);
        sessionVersion = sessionCipher.getSessionVersion();
      } else if (envelope.isUnidentifiedSender()) {
          System.err.println("SSC, UnidentifiedCenter! create SignalSealedSessionCipher");
        SignalSealedSessionCipher sealedSessionCipher = new SignalSealedSessionCipher(sessionLock, new SealedSessionCipher(signalProtocolStore, localAddress.getUuid().orElse(null), localAddress.getNumber().orElse(null), 1));
          System.err.println("SSC, sealedSessionCipher created, ask to decrypt now");
        DecryptionResult          result              = sealedSessionCipher.decrypt(certificateValidator, ciphertext, envelope.getServerReceivedTimestamp());
          System.err.println("SSC, sealedsessioncipher decrypted!");
        SignalServiceAddress      resultAddress       = new SignalServiceAddress(UuidUtil.parse(result.getSenderUuid().orElse(null)), result.getSenderE164());
        SignalProtocolAddress     protocolAddress     = getPreferredProtocolAddress(signalProtocolStore, resultAddress, result.getDeviceId());

        paddedMessage  = result.getPaddedMessage();
        metadata       = new SignalServiceMetadata(resultAddress, result.getDeviceId(), envelope.getTimestamp(), envelope.getServerReceivedTimestamp(), envelope.getServerDeliveredTimestamp(), true);
        sessionVersion = sealedSessionCipher.getSessionVersion(protocolAddress);
      } else {
        throw new InvalidMetadataMessageException("Unknown type: " + envelope.getType());
      }

      PushTransportDetails transportDetails = new PushTransportDetails(sessionVersion);
      byte[]               data             = transportDetails.getStrippedPaddingMessageBody(paddedMessage);

      return new Plaintext(metadata, data);
    } catch (DuplicateMessageException e) {
      throw new ProtocolDuplicateMessageException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    } catch (LegacyMessageException e) {
      throw new ProtocolLegacyMessageException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    } catch (InvalidMessageException e) {
      throw new ProtocolInvalidMessageException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    } catch (InvalidKeyIdException e) {
      throw new ProtocolInvalidKeyIdException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    } catch (InvalidKeyException e) {
      throw new ProtocolInvalidKeyException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    } catch (UntrustedIdentityException e) {
      throw new ProtocolUntrustedIdentityException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    } catch (InvalidVersionException e) {
      throw new ProtocolInvalidVersionException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    } catch (NoSessionException e) {
      throw new ProtocolNoSessionException(e, envelope.getSourceIdentifier(), envelope.getSourceDevice());
    }
  }

  private static SignalProtocolAddress getPreferredProtocolAddress(SignalProtocolStore store, SignalServiceAddress address, int sourceDevice) {
    SignalProtocolAddress uuidAddress = address.getUuid().isPresent() ? new SignalProtocolAddress(address.getUuid().get().toString(), sourceDevice) : null;
    SignalProtocolAddress e164Address = address.getNumber().isPresent() ? new SignalProtocolAddress(address.getNumber().get(), sourceDevice) : null;

    if (uuidAddress != null && store.containsSession(uuidAddress)) {
      return uuidAddress;
    } else if (e164Address != null && store.containsSession(e164Address)) {
      return e164Address;
    } else {
      return new SignalProtocolAddress(address.getLegacyIdentifier(), sourceDevice);
    }
  }

  private static class Plaintext {
    private final SignalServiceMetadata metadata;
    private final byte[]   data;

    private Plaintext(SignalServiceMetadata metadata, byte[] data) {
      this.metadata = metadata;
      this.data     = data;
    }

    public SignalServiceMetadata getMetadata() {
      return metadata;
    }

    public byte[] getData() {
      return data;
    }
  }

}
