package org.ethereum.beacon.ssz.visitor;

public enum SSZCompositeType {
  Container,
  BasicVector,
  BasicList,
  CompositeVector,
  CompositeList;

  boolean isVariableSize() {
    return this == BasicList || this == CompositeList;
  }

  boolean isBasicElementType() {
    return this == BasicList || this == BasicVector;
  }
}
