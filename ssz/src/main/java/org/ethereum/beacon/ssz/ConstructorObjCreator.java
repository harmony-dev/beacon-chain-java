package org.ethereum.beacon.ssz;

import org.javatuples.Pair;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Tries to create object instance by one constructor with all input fields included.
 */
public class ConstructorObjCreator implements ObjectCreator {

  /**
   * <p>Creates instance of object using field -> value data</p>
   * @param clazz             Object class
   * @param fieldValuePairs   Field -> value info
   * @return Pair[success or not, created instance if success or null otherwise]
   */
  @Override
  public Pair<Boolean, Object> createObject(Class clazz, List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs) {
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
}
