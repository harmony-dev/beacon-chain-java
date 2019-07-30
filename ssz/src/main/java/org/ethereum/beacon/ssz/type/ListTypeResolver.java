package org.ethereum.beacon.ssz.type;

import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.access.AccessorResolver;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.access.list.BitlistAccessor;
import org.ethereum.beacon.ssz.type.list.BytesVectorResolver;
import org.ethereum.beacon.ssz.type.list.MaxSizeAnnotationResolver;
import org.ethereum.beacon.ssz.type.list.SSZBitListType;
import org.ethereum.beacon.ssz.type.list.SSZListType;
import org.ethereum.beacon.ssz.type.list.VectorSizeAnnotationResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListTypeResolver implements TypeResolver {

  private final AccessorResolver accessorResolver;
  private final TypeResolver baseTypeResolver;
  private final List<TypeParameterResolver<Number>> vectorSizeResolvers = new ArrayList<>();
  private final List<TypeParameterResolver<Number>> maxSizeResolvers = new ArrayList<>();

  ListTypeResolver(
      AccessorResolver accessorResolver,
      TypeResolver baseTypeResolver,
      ExternalVarResolver externalVarResolver) {
    this.accessorResolver = accessorResolver;
    this.baseTypeResolver = baseTypeResolver;
    vectorSizeResolvers.add(new VectorSizeAnnotationResolver(externalVarResolver));
    vectorSizeResolvers.add(new BytesVectorResolver());
    maxSizeResolvers.add(new MaxSizeAnnotationResolver(externalVarResolver));
  }

  @Override
  public SSZType resolveSSZType(SSZField descriptor) {
    Optional<SSZListAccessor> listAccessor = accessorResolver.resolveListAccessor(descriptor);
    if (listAccessor.isPresent()) {
      SSZListAccessor accessor = listAccessor.get();
      if (accessor instanceof BitlistAccessor) {
        return new SSZBitListType(
            descriptor,
            baseTypeResolver,
            accessor,
            getVectorSize(descriptor),
            getMaxSize(descriptor));
      } else {
        return new SSZListType(
            descriptor,
            baseTypeResolver,
            accessor,
            getVectorSize(descriptor),
            getMaxSize(descriptor));
      }
    }

    return null;
  }

  private int getVectorSize(SSZField descriptor) {
    for (TypeParameterResolver<Number> resolver : vectorSizeResolvers) {
      Optional<Number> res = resolver.resolveTypeParameter(descriptor);
      if (res.isPresent()) {
        return res.get().intValue();
      }
    }

    return SSZType.VARIABLE_SIZE;
  }

  private long getMaxSize(SSZField descriptor) {
    for (TypeParameterResolver<Number> resolver : maxSizeResolvers) {
      Optional<Number> res = resolver.resolveTypeParameter(descriptor);
      if (res.isPresent()) {
        return res.get().longValue();
      }
    }

    return SSZType.VARIABLE_SIZE;
  }
}
