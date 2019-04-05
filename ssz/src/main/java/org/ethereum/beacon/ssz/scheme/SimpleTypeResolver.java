package org.ethereum.beacon.ssz.scheme;

import java.util.Optional;
import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.SSZContainerAccessor;
import org.ethereum.beacon.ssz.type.SSZListAccessor;

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
    SSZCodec codec = accessorResolver.resolveBasicTypeCodec(descriptor);
    if (codec != null) {
      return new SSZBasicType(descriptor, codec);
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
    if (descriptor.fieldAnnotation == null) {
      return -1;
    }
    String vectorSize = descriptor.fieldAnnotation.vectorSize();
    if (vectorSize.isEmpty()) {
      return -1;
    }
    if (vectorSize.startsWith("${") && vectorSize.endsWith("}")) {
      return externalVarResolver
          .resolveMandatory(vectorSize.substring(2, vectorSize.length() - 1), Number.class)
          .intValue();
    }
    try {
      return Integer.parseInt(vectorSize);
    } catch (NumberFormatException e) {
      throw new RuntimeException(
          "Unrecognized vectorSize attribute (expected either int or '${varName}'): '"
              + vectorSize + "'");
    }
  }
}
