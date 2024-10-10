package org.whispersystems.signalservice.api.message;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.whispersystems.signalservice.internal.push.LegacySignalServiceProtos;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

/**
 *
 * @author johan
 */
public class CustomMessageTest {

    @Test
    public void newLegacyMessage() throws InvalidProtocolBufferException {
        SignalServiceProtos.DataMessage.Builder builder = SignalServiceProtos.DataMessage.newBuilder();
        builder.setBody("foo");
        SignalServiceProtos.DataMessage dataMessage = builder.build();
        ByteString byteString = dataMessage.toByteString();
        LegacySignalServiceProtos.DataMessage parsed = LegacySignalServiceProtos.DataMessage.parseFrom(byteString);
        assertEquals(parsed.getBody(), "foo");
    }

    @Test
    public void canvasMessage() throws InvalidProtocolBufferException {
        String content = "some canvas content";
        SignalServiceProtos.CanvasMessage.Builder builder = SignalServiceProtos.CanvasMessage.newBuilder();
        builder.setContent(content);
        SignalServiceProtos.CanvasMessage canvas = builder.build();
        ByteString byteString = canvas.toByteString();
        SignalServiceProtos.CanvasMessage parsed = SignalServiceProtos.CanvasMessage.parseFrom(byteString);
        assertEquals(content, parsed.getContent());
    }
}