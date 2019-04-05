package org.ethereum.beacon.ssz.visitor;

import java.util.function.Predicate;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.javatuples.Pair;

public class SSZVisitorHost {

  private Predicate<Pair<Class<?>, SSZField>> containerMembersFilter = i -> true;

  public void setContainerMembersFilter(
      Predicate<Pair<Class<?>, SSZField>> containerMembersFilter) {
    this.containerMembersFilter = containerMembersFilter;
  }

  public  <ResultType, ParamType> ResultType handleAny(
      SSZType type, ParamType value, SSZVisitor<ResultType, ParamType> visitor) {

    if (type.isBasicType()) {
      return visitor.visitBasicValue((SSZBasicType) type, value);
    } else if (type.isList()) {
      SSZListType listType = (SSZListType) type;
      return visitor.visitList(listType, value, (idx, param) ->
        handleAny(listType.getElementType(), param, visitor));

    } else if (type.isContainer()) {
      SSZContainerType containerType = (SSZContainerType) type;
      // TODO handle filter
      return visitor.visitComposite(containerType, value, (idx, param) ->
          handleAny(containerType.getChildTypes().get(idx), param, visitor));
    } else {
      throw new IllegalArgumentException("Unknown type: " + type);
    }
  }
}
