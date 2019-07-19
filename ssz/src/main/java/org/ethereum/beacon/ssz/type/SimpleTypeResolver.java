package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.access.AccessorResolver;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.access.SSZUnionAccessor;
import org.ethereum.beacon.ssz.access.list.BitlistAccessor;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;

import java.util.Optional;

public class SimpleTypeResolver implements TypeResolver {

  private final AccessorResolver accessorResolver;
  private final ExternalVarResolver externalVarResolver;

  public SimpleTypeResolver(AccessorResolver accessorResolver,
      ExternalVarResolver externalVarResolver) {
    this.accessorResolver = accessorResolver;
    this.externalVarResolver = externalVarResolver;
  }

  @Override
  public SSZType resolveSSZType(SSZField descriptor) {
    Optional<SSZBasicAccessor> codec = accessorResolver.resolveBasicAccessor(descriptor);
    if (codec.isPresent()) {
      return new SSZBasicType(descriptor, codec.get());
    }

    Optional<SSZListAccessor> listAccessor = accessorResolver.resolveListAccessor(descriptor);
    if (listAccessor.isPresent()) {
      SSZListAccessor accessor = listAccessor.get();
      if (accessor instanceof BitlistAccessor) {
        return new SSZBitListType(
            descriptor, this, accessor, getVectorSize(descriptor), getMaxSize(descriptor));
      } else {
        return new SSZListType(
            descriptor, this, accessor, getVectorSize(descriptor), getMaxSize(descriptor));
      }
    }

    Optional<SSZUnionAccessor> unionAccessor = accessorResolver
        .resolveUnionAccessor(descriptor);
    if (unionAccessor.isPresent()) {
      return new SSZUnionType(unionAccessor.get(), descriptor, this);
    }

    Optional<SSZContainerAccessor> containerAccessor = accessorResolver
        .resolveContainerAccessor(descriptor);
    if (containerAccessor.isPresent()) {
      return new SSZContainerType(this, descriptor, containerAccessor.get());
    }

    throw new SSZSchemeException("Couldn't resolve type for descriptor " + descriptor);
  }

  protected int getVectorSize(SSZField descriptor) {
    if (descriptor.getFieldAnnotation() != null) {
      int vectorSize = descriptor.getFieldAnnotation().vectorLength();
      if (vectorSize > 0) {
        return vectorSize;
      }
      String vectorSizeVar = descriptor.getFieldAnnotation().vectorLengthVar();
      if (!vectorSizeVar.isEmpty()) {
        return externalVarResolver.resolveOrThrow(vectorSizeVar, Number.class).intValue();
      }
    }
    // TODO: refactor meeeeee pleeeease
    SSZSerializable annotation = descriptor.getRawClass().getAnnotation(SSZSerializable.class);
    if (Bytes1.class.isAssignableFrom(descriptor.getRawClass())
        || (annotation != null && Bytes1.class.isAssignableFrom(annotation.serializeAs()))) {
      return 1;
    }
    if (Bytes4.class.isAssignableFrom(descriptor.getRawClass())
        || (annotation != null && Bytes4.class.isAssignableFrom(annotation.serializeAs()))) {
      return 4;
    }
    if (Bytes32.class.isAssignableFrom(descriptor.getRawClass())
        || (annotation != null && Bytes32.class.isAssignableFrom(annotation.serializeAs()))) {
      return 32;
    }
    if (Bytes48.class.isAssignableFrom(descriptor.getRawClass())
        || (annotation != null && Bytes48.class.isAssignableFrom(annotation.serializeAs()))) {
      return 48;
    }
    if (Bytes96.class.isAssignableFrom(descriptor.getRawClass())
        || (annotation != null && Bytes96.class.isAssignableFrom(annotation.serializeAs()))) {
      return 96;
    }

    return SSZType.VARIABLE_SIZE;
  }

  protected long getMaxSize(SSZField descriptor) {
    if (descriptor.getFieldAnnotation() == null) {
      return SSZType.VARIABLE_SIZE;
    }
    long maxSize = descriptor.getFieldAnnotation().maxSize();
    if (maxSize > 0) {
      return maxSize;
    }
    String maxSizeVar = descriptor.getFieldAnnotation().maxSizeVar();
    if (!maxSizeVar.isEmpty()) {
      return externalVarResolver
          .resolveOrThrow(maxSizeVar, Number.class)
          .longValue();
    }

    return SSZType.VARIABLE_SIZE;
  }
}
