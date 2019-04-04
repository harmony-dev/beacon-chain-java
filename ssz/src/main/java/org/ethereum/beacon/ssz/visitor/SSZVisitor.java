package org.ethereum.beacon.ssz.visitor;

import java.util.function.Function;
import org.ethereum.beacon.ssz.scheme.SSZBasicType;
import org.ethereum.beacon.ssz.scheme.SSZCompositeType;

public interface SSZVisitor<ResultType> {

  ResultType visitBasicValue(SSZBasicType descriptor, Object value);

  ResultType visitComposite(
      SSZCompositeType type, Object rawValue, Function<Integer, ResultType> childVisitor);
}


