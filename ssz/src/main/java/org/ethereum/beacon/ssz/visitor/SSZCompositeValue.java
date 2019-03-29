package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZCompositeValue {

  SSZCompositeType getCompositeType();

  SSZField getElementType();

  Object getRawValue();

  long getChildCount();
}
