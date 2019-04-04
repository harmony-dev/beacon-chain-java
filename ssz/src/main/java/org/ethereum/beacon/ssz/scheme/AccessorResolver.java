package org.ethereum.beacon.ssz.scheme;

import java.util.Optional;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.SSZContainerAccessor;
import org.ethereum.beacon.ssz.type.SSZListAccessor;

public interface AccessorResolver {

  SSZCodec resolveBasicTypeCodec(SSZField field);

  Optional<SSZListAccessor> resolveListAccessor(SSZField field);

  Optional<SSZContainerAccessor> resolveContainerAccessor(SSZField field);
}
