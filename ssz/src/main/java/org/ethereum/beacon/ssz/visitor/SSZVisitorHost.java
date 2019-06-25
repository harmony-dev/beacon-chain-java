package org.ethereum.beacon.ssz.visitor;

import static org.ethereum.beacon.ssz.type.SSZType.Type.BASIC;
import static org.ethereum.beacon.ssz.type.SSZType.Type.CONTAINER;
import static org.ethereum.beacon.ssz.type.SSZType.Type.LIST;
import static org.ethereum.beacon.ssz.type.SSZType.Type.UNION;
import static org.ethereum.beacon.ssz.type.SSZType.Type.VECTOR;

import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.SSZUnionType;

public class SSZVisitorHost {

  public  <ResultType, ParamType> ResultType handleAny(
      SSZType type, ParamType value, SSZVisitor<ResultType, ParamType> visitor) {

    if (type.getType() == BASIC) {
      return visitor.visitBasicValue((SSZBasicType) type, value);
    } else if (type.getType() == LIST || type.getType() == VECTOR) {
      SSZListType listType = (SSZListType) type;
      return visitor.visitList(listType, value, (idx, param) ->
        handleAny(listType.getElementType(), param, visitor));
    } else if (type.getType() == UNION) {
      SSZUnionType unionType = (SSZUnionType) type;
      return visitor.visitUnion(unionType, value, (idx, param) ->
          handleAny(unionType.getChildTypes().get(idx), param, visitor));
    } else if (type.getType() == CONTAINER) {
      SSZContainerType containerType = (SSZContainerType) type;
      return visitor.visitComposite(containerType, value, (idx, param) ->
          handleAny(containerType.getChildTypes().get(idx), param, visitor));
    } else {
      throw new IllegalArgumentException("Unknown type: " + type);
    }
  }

  public  <ResultType, ParamType> ResultType handleSubList(
      SSZListType type, ParamType value, int startIdx, int len, SSZVisitor<ResultType, ParamType> visitor) {

    return visitor.visitSubList(type, value, startIdx, len, (idx, param) ->
        handleAny(type.getElementType(), param, visitor));
  }
}
