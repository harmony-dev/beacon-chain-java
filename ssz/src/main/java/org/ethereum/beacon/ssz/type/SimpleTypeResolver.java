package org.ethereum.beacon.ssz.type;

import java.util.Optional;
import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.access.AccessorResolver;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZListAccessor;

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
      return new SSZListType(descriptor, this, listAccessor.get(), getVectorSize(descriptor));
    }

    Optional<SSZContainerAccessor> containerAccessor = accessorResolver
        .resolveContainerAccessor(descriptor);
    if (containerAccessor.isPresent()) {
      return new SSZContainerType(this, descriptor, containerAccessor.get());
    }

    throw new SSZSchemeException("Couldn't resolve type for descriptor " + descriptor);
  }

  protected int getVectorSize(SSZField descriptor) {
    if (descriptor.getFieldAnnotation() == null) {
      return -1;
    }
    int vectorSize = descriptor.getFieldAnnotation().vectorSize();
    if (vectorSize > 0) {
      return vectorSize;
    }
    String vectorSizeVar = descriptor.getFieldAnnotation().vectorSizeVar();
    if (!vectorSizeVar.isEmpty()) {
      return externalVarResolver
              .resolveMandatory(vectorSizeVar, Number.class)
              .intValue();
    }

    return -1;
  }
}
