/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package org.whispersystems.signalservice.internal.util;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.signalservice.api.util.UuidUtil;
import org.whispersystems.util.Base64;

import java.io.IOException;
import java.util.UUID;
import org.whispersystems.signalservice.api.push.exceptions.MalformedResponseException;

public class JsonUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static String toJson(Object object) {
    try {
      String answer = objectMapper.writeValueAsString(object);
      return answer;
    } catch (JsonProcessingException e) {
        e.printStackTrace();
      return "";
    }
  }

  public static <T> T fromJson(String json, Class<T> clazz)
      throws IOException
  {
    return objectMapper.readValue(json, clazz);
  }
  
    public static <T> T fromJson(String json, TypeReference<T> typeRef)
      throws IOException
  {
    return objectMapper.readValue(json, typeRef);
  }

  public static <T> T fromJsonResponse(String json, TypeReference<T> typeRef)
      throws MalformedResponseException
  {
    try {
      return JsonUtil.fromJson(json, typeRef);
    } catch (IOException e) {
      throw new MalformedResponseException("Unable to parse entity", e);
    }
  }

  public static <T> T fromJsonResponse(String body, Class<T> clazz)
      throws MalformedResponseException
  {
    try {
      return JsonUtil.fromJson(body, clazz);
    } catch (IOException e) {
      throw new MalformedResponseException("Unable to parse entity", e);
    }
  }

    public static class IdentityKeySerializer extends JsonSerializer<IdentityKey> {
    @Override
    public void serialize(IdentityKey value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException
    {
      gen.writeString(Base64.encodeBytesWithoutPadding(value.serialize()));
    }
  }

  public static class IdentityKeyDeserializer extends JsonDeserializer<IdentityKey> {
    @Override
    public IdentityKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      try {
        return new IdentityKey(Base64.decodeWithoutPadding(p.getValueAsString()), 0);
      } catch (InvalidKeyException e) {
        throw new IOException(e);
      }
    }
  }

  public static class UuidSerializer extends JsonSerializer<UUID> {
    @Override
    public void serialize(UUID value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException
    {
      gen.writeString(value.toString());
    }
  }

  public static class UuidDeserializer extends JsonDeserializer<UUID> {
    @Override
    public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return UuidUtil.parseOrNull(p.getValueAsString());
    }
  }
}
