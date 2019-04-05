package org.ethereum.beacon.ssz.visitor;

import java.util.function.BiFunction;
import org.ethereum.beacon.ssz.scheme.SSZBasicType;
import org.ethereum.beacon.ssz.scheme.SSZCompositeType;
import org.ethereum.beacon.ssz.scheme.SSZContainerType;
import org.ethereum.beacon.ssz.scheme.SSZListType;

public interface SSZVisitor<ResultType, ParamType> {

  ResultType visitBasicValue(SSZBasicType sszType, ParamType param);

  default ResultType visitList(
      SSZListType type, ParamType param, BiFunction<Integer, ParamType, ResultType> childVisitor) {
    return visitComposite(type, param, childVisitor);
  }

  default ResultType visitContainer(
      SSZContainerType type, ParamType param, BiFunction<Integer, ParamType, ResultType> childVisitor) {
    return visitComposite(type, param, childVisitor);
  }

  default ResultType visitComposite(
      SSZCompositeType type, ParamType param, BiFunction<Integer, ParamType, ResultType> childVisitor) {
    throw new UnsupportedOperationException(
        "You should either implement visitComposite() or visitList() + visitContainer()");
  }
}
