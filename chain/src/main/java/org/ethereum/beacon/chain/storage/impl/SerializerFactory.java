package org.ethereum.beacon.chain.storage.impl;

import java.util.function.Function;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.ssz.SSZBuilder;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface SerializerFactory {

  <T> Function<BytesValue, T> getDeserializer(Class<? extends T> objectClass);

  <T> Function<T, BytesValue> getSerializer(Class<? extends T> objectClass);

  static SerializerFactory createSSZ(SpecConstants specConstants) {
    return new SSZSerializerFactory(new SSZBuilder()
            .withExternalVarResolver(new SpecConstantsResolver(specConstants))
            .withExtraObjectCreator(SpecConstants.class, specConstants)
            .buildSerializer());
  }
}
