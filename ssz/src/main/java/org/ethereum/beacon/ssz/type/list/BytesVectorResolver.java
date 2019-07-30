package org.ethereum.beacon.ssz.type.list;

import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.TypeParameterResolver;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;

import java.util.Optional;

/**
 * Resolves {@link Bytes1}, {@link Bytes4}, {@link Bytes32}, {@link Bytes48}, {@link Bytes96} using
 * type or annotation
 */
public class BytesVectorResolver implements TypeParameterResolver<Number> {
  @Override
  public Optional<Number> resolveTypeParameter(SSZField descriptor) {

    // By class
    if (Bytes1.class.isAssignableFrom(descriptor.getRawClass())) {
      return Optional.of(1);
    }
    if (Bytes4.class.isAssignableFrom(descriptor.getRawClass())) {
      return Optional.of(4);
    }
    if (Bytes32.class.isAssignableFrom(descriptor.getRawClass())) {
      return Optional.of(32);
    }
    if (Bytes48.class.isAssignableFrom(descriptor.getRawClass())) {
      return Optional.of(48);
    }
    if (Bytes96.class.isAssignableFrom(descriptor.getRawClass())) {
      return Optional.of(96);
    }

    // By annotation
    SSZSerializable annotation = descriptor.getRawClass().getAnnotation(SSZSerializable.class);
    if (annotation == null || annotation.serializeAs() == void.class) {
      return Optional.empty();
    }
    if (Bytes1.class.isAssignableFrom(annotation.serializeAs())) {
      return Optional.of(1);
    }
    if (Bytes4.class.isAssignableFrom(annotation.serializeAs())) {
      return Optional.of(4);
    }
    if (Bytes32.class.isAssignableFrom(annotation.serializeAs())) {
      return Optional.of(32);
    }
    if (Bytes48.class.isAssignableFrom(annotation.serializeAs())) {
      return Optional.of(48);
    }
    if (Bytes96.class.isAssignableFrom(annotation.serializeAs())) {
      return Optional.of(96);
    }

    return Optional.empty();
  }
}
