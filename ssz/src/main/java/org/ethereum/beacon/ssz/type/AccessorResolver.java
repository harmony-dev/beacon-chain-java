package org.ethereum.beacon.ssz.type;

import java.util.Optional;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZCodec;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZListAccessor;

public interface AccessorResolver {

  SSZCodec resolveBasicTypeCodec(SSZField field);

  Optional<SSZListAccessor> resolveListAccessor(SSZField field);

  Optional<SSZContainerAccessor> resolveContainerAccessor(SSZField field);
}
