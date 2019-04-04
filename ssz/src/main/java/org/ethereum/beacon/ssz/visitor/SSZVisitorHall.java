package org.ethereum.beacon.ssz.visitor;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.ethereum.beacon.ssz.SSZCodecResolver;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.scheme.SSZBasicType;
import org.ethereum.beacon.ssz.scheme.SSZContainerType;
import org.ethereum.beacon.ssz.scheme.SSZListType;
import org.ethereum.beacon.ssz.scheme.SSZType;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.javatuples.Pair;

public class SSZVisitorHall {

  private Predicate<Pair<Class<?>, SSZField>> containerMembersFilter = i -> true;

  public void setContainerMembersFilter(
      Predicate<Pair<Class<?>, SSZField>> containerMembersFilter) {
    this.containerMembersFilter = containerMembersFilter;
  }

  public  <ResultType> ResultType handleAny(SSZType type, Object value, SSZVisitor<ResultType> visitor) {
    if (type.isBasicType()) {
      return visitor.visitBasicValue((SSZBasicType) type, value);
    } else if (type.isList()) {
      SSZListType listType = (SSZListType) type;
      return visitor.visitComposite(listType, value, idx ->
        handleAny(listType.getElementType(), listType.getChild(value, idx), visitor));

    } else if (type.isContainer()) {
      SSZContainerType containerType = (SSZContainerType) type;
      // TODO handle filter
      return visitor.visitComposite(containerType, value, idx ->
          handleAny(containerType.getChildTypes().get(idx),
              containerType.getChild(value, idx), visitor));
    } else {
      throw new IllegalArgumentException("Unknown type: " + type);
    }
  }
}
