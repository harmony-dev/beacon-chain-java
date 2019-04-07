package org.ethereum.beacon.chain.storage.impl;

import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZBuilder;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface SerializerFactory {

  <T> Function<BytesValue, T> getDeserializer(Class<T> objectClass);

  <T> Function<T, BytesValue> getSerializer(Class<T> objectClass);

  static SerializerFactory createSSZ() {
    return new SSZSerializerFactory(new SSZBuilder().buildSerializer());
  }
}
