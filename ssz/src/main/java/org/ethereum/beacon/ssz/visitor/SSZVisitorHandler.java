package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.scheme.SSZType;

public interface SSZVisitorHandler<ResultType> {
  ResultType visitAny(SSZType descriptor, Object value);

  default ResultType visit(Object input) {
    return visit(input, input.getClass());
  }

  <C> ResultType visit(C input, Class<? extends C> clazz);
}
