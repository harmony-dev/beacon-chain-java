package org.ethereum.beacon.ssz.type.list;

import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.TypeParameterResolver;

import java.util.Optional;

public class MaxSizeAnnotationResolver implements TypeParameterResolver<Number> {

  private final ExternalVarResolver externalVarResolver;

  public MaxSizeAnnotationResolver(ExternalVarResolver externalVarResolver) {
    this.externalVarResolver = externalVarResolver;
  }

  @Override
  public Optional<Number> resolveTypeParameter(SSZField descriptor) {
    if (descriptor.getFieldAnnotation() != null) {
      long maxSize = descriptor.getFieldAnnotation().maxSize();
      if (maxSize > 0) {
        return Optional.of(maxSize);
      }
      String maxSizeVar = descriptor.getFieldAnnotation().maxSizeVar();
      if (!maxSizeVar.isEmpty()) {
        return Optional.of(externalVarResolver.resolveOrThrow(maxSizeVar, Number.class));
      }
    }

    return Optional.empty();
  }
}
