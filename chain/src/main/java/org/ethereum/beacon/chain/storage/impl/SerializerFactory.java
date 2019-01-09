package org.ethereum.beacon.chain.storage.impl;

import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

public interface SerializerFactory {

  <T> Function<BytesValue, T> getDeserializer(Class<T> objectClass);

  <T> Function<T, BytesValue> getSerializer(Class<T> objectClass);
}
