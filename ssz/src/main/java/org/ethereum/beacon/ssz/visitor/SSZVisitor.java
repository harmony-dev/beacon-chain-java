package org.ethereum.beacon.ssz.visitor;

import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZVisitor<ResultType> {

  ResultType visitBasicValue(SSZField descriptor, Object value);

  ResultType visitComposite(SSZCompositeValue value, Function<Long, ResultType> childVisitor);
}


