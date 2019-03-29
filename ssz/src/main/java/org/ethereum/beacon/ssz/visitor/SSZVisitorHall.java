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
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.SSZListAccessor;
import org.javatuples.Pair;

public class SSZVisitorHall {

  static class SSZCompositeValueImpl implements SSZCompositeValue {
    SSZCompositeType type;
    SSZField descriptor;
    Object rawValue;
    long childCount;

    public SSZCompositeValueImpl(SSZCompositeType type,
        SSZField descriptor, Object rawValue, long childCount) {
      this.type = type;
      this.descriptor = descriptor;
      this.rawValue = rawValue;
      this.childCount = childCount;
    }

    @Override
    public SSZCompositeType getCompositeType() {
      return type;
    }

    @Override
    public SSZField getElementType() {
      return descriptor;
    }

    @Override
    public Object getRawValue() {
      return rawValue;
    }

    @Override
    public long getChildCount() {
      return childCount;
    }
  }

  private final  SSZSchemeBuilder schemeBuilder;

  private final SSZCodecResolver codecResolver;

  private Predicate<Pair<Class<?>, SSZField>> containerMembersFilter = i -> true;

  public SSZVisitorHall(SSZSchemeBuilder schemeBuilder,
      SSZCodecResolver codecResolver) {
    this.schemeBuilder = schemeBuilder;
    this.codecResolver = codecResolver;
  }

  public void setContainerMembersFilter(
      Predicate<Pair<Class<?>, SSZField>> containerMembersFilter) {
    this.containerMembersFilter = containerMembersFilter;
  }

  /**
   * Serializes input to byte[] data
   *
   * @param inputObject input value
   * @param inputClazz Class of value
   * @return SSZ serialization
   */
  private <C, ResultType> ResultType handleContainer(
      @Nullable C inputObject, Class<? extends C> inputClazz, SSZVisitor<ResultType> visitor) {
    checkSSZSerializableAnnotation(inputClazz);

    Object input;
    Class<?> clazz;
    if (!inputClazz.getAnnotation(SSZSerializable.class).instanceGetter().isEmpty()) {
      try {
        Method instanceGetter =
            inputClazz.getMethod(inputClazz.getAnnotation(SSZSerializable.class).instanceGetter());
        input = instanceGetter.invoke(inputObject);
        clazz = input.getClass();
      } catch (Exception e) {
        throw new RuntimeException("Error processing SSZSerializable.instanceGetter attribute", e);
      }
    } else {
      input = inputObject;
      clazz = inputClazz;
    }

    // Fill up map with all available method getters
    Map<String, Method> getters = new HashMap<>();
    try {
      for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        getters.put(pd.getReadMethod().getName(), pd.getReadMethod());
      }
    } catch (IntrospectionException e) {
      String error = String.format("Couldn't enumerate all getters in class %s", clazz.getName());
      throw new RuntimeException(error, e);
    }

    // Encode object fields one by one
    List<SSZField> filteredFields = schemeBuilder.build(clazz).getFields().stream()
            .filter(f -> containerMembersFilter.test(Pair.with(clazz, f)))
            .collect(Collectors.toList());

    SSZCompositeValueImpl compositeValue = new SSZCompositeValueImpl(
        SSZCompositeType.Container, null,
        inputObject, filteredFields.size());

    return visitor.visitComposite(compositeValue, idx -> {
      SSZField field = filteredFields.get(idx.intValue());
      Method getter = getters.get(field.getter);
      Object rawVal;
      try {
        if (getter != null) { // We have getter
          rawVal = getter.invoke(input);
        } else { // Trying to access field directly
          rawVal = clazz.getField(field.name).get(input);
        }
      } catch (Exception e) {
        throw new SSZSchemeException(String.format("Failed to get value from field %s, your should either have public field or public getter for it", field.name));
      }
      return handleAny(field, rawVal, visitor);
    });
  }

  public  <ResultType> ResultType handleAny(
      SSZSchemeBuilder.SSZScheme.SSZField field, Object value, SSZVisitor<ResultType> visitor) {
    SSZCodec encoder = codecResolver.resolveBasicValueCodec(field);
    SSZListAccessor listAccessor = codecResolver.resolveListValueAccessor(field);

    if (encoder != null) {
      return visitor.visitBasicValue(field, value);
    } else if (listAccessor != null) {

      // TODO handle vector types
      boolean isVector = listAccessor.isVector(field);
      SSZField listElementType = listAccessor.getListElementType(field);
      SSZCodec listElementBasicCodec = codecResolver.resolveBasicValueCodec(listElementType);

      if (listElementBasicCodec != null) {
        // basic list/vector type
        SSZCompositeValueImpl compositeValue = new SSZCompositeValueImpl(
            isVector ? SSZCompositeType.BasicVector : SSZCompositeType.BasicList,
            field, value, listAccessor.getChildCount(value));
        return visitor.visitComposite(compositeValue, idx -> {
          return visitor.visitBasicValue(field, listAccessor.getChild(value, idx));
        });
      } else {
        // composite list/vector type
        SSZCompositeValueImpl compositeValue = new SSZCompositeValueImpl(
            isVector ? SSZCompositeType.CompositeVector : SSZCompositeType.CompositeList,
            field, value, listAccessor.getChildCount(value));
        return visitor.visitComposite(compositeValue, idx -> {
          return handleContainer(listAccessor.getChild(value, idx), field.fieldType, visitor);
        });
      }
    } else {
      return handleContainer(value, field.fieldType, visitor);
    }
  }

  static void checkSSZSerializableAnnotation(Class clazz) {
    if (!clazz.isAnnotationPresent(SSZSerializable.class)) {
      String error =
          String.format(
              "Serializer doesn't know how to handle class of type %s. Maybe you forget to "
                  + "annotate it with SSZSerializable?",
              clazz);
      throw new SSZSchemeException(error);
    }
  }
}
