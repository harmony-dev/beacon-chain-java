package org.ethereum.beacon.ssz;

import org.javatuples.Pair;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SSZModelCreator implements SSZModelFactory {

  public Object create(Class clazz, List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs) {
    Pair<Boolean, Object> constructorAttempt = createInstanceWithConstructor(clazz, fieldValuePairs);

    Object result;
    if (!constructorAttempt.getValue0()) {
      List<SSZSchemeBuilder.SSZScheme.SSZField> fields = fieldValuePairs.stream()
          .map(Pair::getValue0)
          .collect(Collectors.toList());
      Object[] values = fieldValuePairs.stream()
          .map(Pair::getValue1)
          .toArray();
      Pair<Boolean, Object> setterAttempt = createInstanceWithSetters(clazz, fields, values);
      if (!setterAttempt.getValue0()) {
        String fieldTypes = Arrays.stream(values)
            .map(v -> v.getClass().toString())
            .collect(Collectors.joining(","));
        String error = String.format("Unable to find appropriate class %s "
            + "construction method with params [%s]."
            + "You should either have constructor with all non-transient fields "
            + "or setters/public fields.", clazz.getName(), fieldTypes);
        throw new SSZSchemeException(error);
      } else {
        result = setterAttempt.getValue1();
      }
    } else {
      result = constructorAttempt.getValue1();
    }

    return result;
  }

  private static Pair<Boolean, Object> createInstanceWithConstructor(Class clazz, List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs) {
    Class[] params = new Class[fieldValuePairs.size()];
    for (int i = 0; i < fieldValuePairs.size(); i++) {
      Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object> pair = fieldValuePairs.get(i);
      SSZSchemeBuilder.SSZScheme.SSZField field = pair.getValue0();
      switch (field.multipleType) {
        case LIST: {
          params[i] = List.class;
          break;
        }
        case ARRAY: {
          params[i] = Array.newInstance(field.type, 0).getClass();
          break;
        }
        default: {
          params[i] = field.type;
          break;
        }
      }
    }
    Object[] values = fieldValuePairs.stream()
        .map(Pair::getValue1)
        .toArray();

    return createInstanceWithConstructor(clazz, params, values);
  }

  private static Pair<Boolean, Object> createInstanceWithConstructor(Class clazz, Class[] params,
                                                                     Object[] values) {
    // Find constructor for params
    Constructor constructor;
    try {
      constructor = clazz.getConstructor(params);
    } catch (NoSuchMethodException e) {
      return new Pair<>(false, null);
    }

    // Invoke constructor using values as params
    Object result;
    try {
      result = constructor.newInstance(values);
    } catch (Exception e) {
      return new Pair<>(false, null);
    }

    return new Pair<>(true, result);
  }

  private static Pair<Boolean, Object> createInstanceWithSetters(
      Class clazz, List<SSZSchemeBuilder.SSZScheme.SSZField> fields, Object[] values) {
    // Find constructor with no params
    Constructor constructor;
    try {
      constructor = clazz.getConstructor();
    } catch (NoSuchMethodException e) {
      return new Pair<>(false, null);
    }

    // Create empty instance
    Object result;
    try {
      result = constructor.newInstance();
    } catch (Exception e) {
      return new Pair<>(false, null);
    }

    Map<String, Method> fieldSetters = new HashMap<>();
    try {
      for (PropertyDescriptor pd: Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        fieldSetters.put(pd.getName(), pd.getWriteMethod());
      }
    } catch (IntrospectionException e) {
      String error = String.format("Couldn't enumerate all setters in class %s", clazz.getName());
      throw new SSZSchemeException(error, e);
    }

    // Fill up field by field
    for (int i = 0; i < fields.size(); ++i) {
      SSZSchemeBuilder.SSZScheme.SSZField currentField = fields.get(i);
      try {   // Try to set by field assignment
        clazz.getField(currentField.name).set(result, values[i]);
      } catch (Exception e) {
        try {    // Try to set using setter
          fieldSetters.get(currentField.name).invoke(result, values[i]);
        } catch (Exception ex) {    // Cannot set the field
          return new Pair<>(false, null);
        }
      }
    }

    return new Pair<>(true, result);
  }
}
