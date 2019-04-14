package org.ethereum.beacon.ssz.access;

import java.util.Optional;

public interface AccessorResolver {

  Optional<SSZBasicAccessor> resolveBasicAccessor(SSZField field);

  Optional<SSZListAccessor> resolveListAccessor(SSZField field);

  Optional<SSZContainerAccessor> resolveContainerAccessor(SSZField field);
}
