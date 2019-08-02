package org.ethereum.beacon.ssz.creator;

import org.ethereum.beacon.ssz.access.SSZField;
import org.javatuples.Pair;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Tries to create object instance by one constructor with all input fields and added extraValue to
 * the end
 */
public class ConstructorExtraObjCreator implements ObjectCreator {
  private final Class extraType;
  private final Object extraValue;

  public ConstructorExtraObjCreator(Class extraType, Object extraValue) {
    this.extraType = extraType;
    this.extraValue = extraValue;
  }

  public <C> C createInstanceWithConstructor(
      Class<? extends C> clazz, Class[] params, Object[] values) {
    // Find constructor for params
    Constructor<? extends C> constructor;
    try {
      Class[] merged = new Class[params.length + 1];
      System.arraycopy(params, 0, merged, 0, params.length);
      merged[params.length] = extraType;
      constructor = clazz.getConstructor(merged);
    } catch (NoSuchMethodException e) {
      return null;
    }

    // Invoke constructor using values as params
    C result;
    try {
      Object[] merged = new Object[values.length + 1];
      System.arraycopy(values, 0, merged, 0, values.length);
      merged[values.length] = extraValue;
      result = constructor.newInstance(merged);
    } catch (Exception e) {
      return null;
    }

    return result;
  }

  /**
   * Creates instance of object using field -> value data
   *
   * @param clazz Object class
   * @param fieldValuePairs Field -> value info
   * @return Pair[success or not, created instance if success or null otherwise]
   */
  @Override
  public <C> C createObject(
      Class<? extends C> clazz, List<Pair<SSZField, Object>> fieldValuePairs) {
    Class[] params = new Class[fieldValuePairs.size()];
    for (int i = 0; i < fieldValuePairs.size(); i++) {
      Pair<SSZField, Object> pair = fieldValuePairs.get(i);
      SSZField field = pair.getValue0();
      params[i] = field.getRawClass();
    }
    Object[] values = fieldValuePairs.stream().map(Pair::getValue1).toArray();

    return createInstanceWithConstructor(clazz, params, values);
  }
}
