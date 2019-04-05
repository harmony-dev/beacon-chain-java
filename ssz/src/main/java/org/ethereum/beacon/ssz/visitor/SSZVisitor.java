package org.ethereum.beacon.ssz.visitor;

import java.util.function.BiFunction;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZCompositeType;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZListType;

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
