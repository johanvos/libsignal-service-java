module org.whispersystems.service {
    requires java.logging;
    requires java.net.http;
    // requires org.whispersystems.metadata;
    // requires org.whispersystems.protocol;
    requires com.google.protobuf;
    requires grpcproxy.client;
    requires libphonenumber;
    // requires okhttp3;
    requires okio;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires libsignal.client;
    exports org.signal.storageservice.protos.groups;
    exports org.signal.storageservice.protos.groups.local;
    exports org.whispersystems.signalservice.internal.configuration;
    exports org.whispersystems.signalservice.internal.push;
    exports org.whispersystems.signalservice.internal.push.exceptions;
    exports org.whispersystems.signalservice.internal.util;
    exports org.whispersystems.signalservice.internal.util.concurrent;
    exports org.whispersystems.signalservice.internal.webrtc;
    exports org.whispersystems.signalservice.internal.websocket;
    exports org.whispersystems.signalservice.api;
    exports org.whispersystems.signalservice.api.crypto;
    exports org.whispersystems.signalservice.api.groupsv2;
    exports org.whispersystems.signalservice.api.messages;
    exports org.whispersystems.signalservice.api.messages.calls;
    exports org.whispersystems.signalservice.api.messages.multidevice;
    exports org.whispersystems.signalservice.api.profiles;
    exports org.whispersystems.signalservice.api.push;
    exports org.whispersystems.signalservice.api.push.exceptions;
    exports org.whispersystems.signalservice.api.storage;
    exports org.whispersystems.signalservice.api.util;
    exports org.whispersystems.signalservice.api.websocket;
    exports org.whispersystems.util;
    exports tokhttp3;
    opens org.whispersystems.signalservice.api.groupsv2 to com.fasterxml.jackson.databind;
    opens org.whispersystems.signalservice.internal.push to com.fasterxml.jackson.databind;
    opens org.whispersystems.signalservice.api.messages.calls to com.fasterxml.jackson.databind;
    opens org.whispersystems.signalservice.api.profiles to com.fasterxml.jackson.databind;
    opens org.whispersystems.signalservice.api.push to com.fasterxml.jackson.databind;
    opens org.whispersystems.signalservice.api.storage to com.fasterxml.jackson.databind;
    opens org.whispersystems.signalservice.internal.storage.protos to com.google.protobuf;
}
