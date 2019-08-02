package org.ethereum.beacon.ssz.type.list;

import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.TypeParameterResolver;

import java.util.Optional;

public class VectorSizeAnnotationResolver implements TypeParameterResolver<Number> {

  private final ExternalVarResolver externalVarResolver;

  public VectorSizeAnnotationResolver(ExternalVarResolver externalVarResolver) {
    this.externalVarResolver = externalVarResolver;
  }

  @Override
  public Optional<Number> resolveTypeParameter(SSZField descriptor) {
    if (descriptor.getFieldAnnotation() != null) {
      int vectorSize = descriptor.getFieldAnnotation().vectorLength();
      if (vectorSize > 0) {
        return Optional.of(vectorSize);
      }
      String vectorSizeVar = descriptor.getFieldAnnotation().vectorLengthVar();
      if (!vectorSizeVar.isEmpty()) {
        return Optional.of(externalVarResolver.resolveOrThrow(vectorSizeVar, Number.class));
      }
    }

    return Optional.empty();
  }
}
