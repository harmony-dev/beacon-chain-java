package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZVisitorHandler<ResultType> {
  ResultType visitAny(SSZField descriptor, Object value);

  default ResultType visitAny(Object value) {
    return visitAny(null, value);
  }

  default ResultType visitComposite(SSZCompositeValue value) {
    return visitAny(value.getElementType(), value.getRawValue());
  }

  default ResultType visit(Object input) {
    return visit(input, input.getClass());
  }

  <C> ResultType visit(C input, Class<? extends C> clazz);
}
