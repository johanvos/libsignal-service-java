/*
 * Copyright (C) 2014-2017 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.api.messages;

import com.google.protobuf.ByteString;
import java.util.Optional;

import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.AttachmentPointer;

/**
 * Represents a received SignalServiceAttachment "handle."  This
 * is a pointer to the actual attachment content, which needs to be
 * retrieved using {@link SignalServiceMessageReceiver#retrieveAttachment(SignalServiceAttachmentPointer, java.io.File, long)}
 *
 * @author Moxie Marlinspike
 */
public class SignalServiceAttachmentPointer extends SignalServiceAttachment {

  private final int                             cdnNumber;
  private final SignalServiceAttachmentRemoteId remoteId;
  private final byte[]                          key;
  private final Optional<Integer>               size;
  private final Optional<byte[]>                preview;
  private final Optional<byte[]>                digest;
  private final Optional<byte[]>                incrementalDigest;
  private final int                             incrementalMacChunkSize;
  private final Optional<String>                fileName;
  private final boolean                         voiceNote;
  private final boolean                         borderless;
  private final boolean                         gif;
  private final int                             width;
  private final int                             height;
  private final Optional<String>                caption;
  private final Optional<String>                blurHash;
  private final long                            uploadTimestamp;

  public SignalServiceAttachmentPointer(int cdnNumber,
                                        SignalServiceAttachmentRemoteId remoteId,
                                        String contentType,
                                        byte[] key,
                                        Optional<Integer> size,
                                        Optional<byte[]> preview,
                                        int width,
                                        int height,
                                        Optional<byte[]> digest,
                                        Optional<byte[]> incrementalDigest,
                                        int incrementalMacChunkSize,
                                        Optional<String> fileName,
                                        boolean voiceNote,
                                        boolean borderless,
                                        boolean gif,
                                        Optional<String> caption,
                                        Optional<String> blurHash,
                                        long uploadTimestamp)
  {
    super(contentType);
    this.cdnNumber               = cdnNumber;
    this.remoteId                = remoteId;
    this.key                     = key;
    this.size                    = size;
    this.preview                 = preview;
    this.width                   = width;
    this.height                  = height;
    this.incrementalMacChunkSize = incrementalMacChunkSize;
    this.digest                  = digest;
    this.incrementalDigest       = incrementalDigest;
    this.fileName                = fileName;
    this.voiceNote               = voiceNote;
    this.borderless              = borderless;
    this.caption                 = caption;
    this.blurHash                = blurHash;
    this.uploadTimestamp         = uploadTimestamp;
    this.gif                     = gif;
  }

  public int getCdnNumber() {
    return cdnNumber;
  }

  public SignalServiceAttachmentRemoteId getRemoteId() {
    return remoteId;
  }

  public byte[] getKey() {
    return key;
  }

  @Override
  public boolean isStream() {
    return false;
  }

  @Override
  public boolean isPointer() {
    return true;
  }

  public Optional<Integer> getSize() {
    return size;
  }

  public Optional<String> getFileName() {
    return fileName;
  }

  public Optional<byte[]> getPreview() {
    return preview;
  }

  public Optional<byte[]> getDigest() {
    return digest;
  }

  public Optional<byte[]> getIncrementalDigest() {
    return incrementalDigest;
  }

  public boolean getVoiceNote() {
    return voiceNote;
  }

  public boolean isBorderless() {
    return borderless;
  }

  public boolean isGif() {
    return gif;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getIncrementalMacChunkSize() {
    return incrementalMacChunkSize;
  }

  public Optional<String> getCaption() {
    return caption;
  }

  public Optional<String> getBlurHash() {
    return blurHash;
  }

  public long getUploadTimestamp() {
    return uploadTimestamp;
  }
  
  public AttachmentPointer.Builder toAttachmentPointerBuilder() {
     SignalServiceProtos.AttachmentPointer.Builder builder = SignalServiceProtos.AttachmentPointer.newBuilder()
                .setCdnNumber(getCdnNumber())
                .setContentType(getContentType())
                .setKey(ByteString.copyFrom(getKey()))
                .setDigest(ByteString.copyFrom(getDigest().get()))
                .setSize(getSize().get())
                .setUploadTimestamp(getUploadTimestamp());

        if (getRemoteId().getV2().isPresent()) {
            builder.setCdnId(getRemoteId().getV2().get());
        }

        if (getRemoteId().getV3().isPresent()) {
            builder.setCdnKey(getRemoteId().getV3().get());
        }

        if (getFileName().isPresent()) {
            builder.setFileName(getFileName().get());
        }

        if (getPreview().isPresent()) {
            builder.setThumbnail(ByteString.copyFrom(getPreview().get()));
        }

        if (getWidth() > 0) {
            builder.setWidth(getWidth());
        }

        if (getHeight() > 0) {
            builder.setHeight(getHeight());
        }

        if (getVoiceNote()) {
            builder.setFlags(SignalServiceProtos.AttachmentPointer.Flags.VOICE_MESSAGE_VALUE);
        }

        if (isBorderless()) {
            builder.setFlags(SignalServiceProtos.AttachmentPointer.Flags.BORDERLESS_VALUE);
        }

        if (getCaption().isPresent()) {
            builder.setCaption(getCaption().get());
        }

        if (getBlurHash().isPresent()) {
            builder.setBlurHash(getBlurHash().get());
        }

        return builder;
}
}
