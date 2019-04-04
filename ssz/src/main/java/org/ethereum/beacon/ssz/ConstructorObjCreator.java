package org.ethereum.beacon.ssz;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.javatuples.Pair;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.List;

/** Tries to create object instance by one constructor with all input fields included. */
public class ConstructorObjCreator implements ObjectCreator {


  public static <C> Pair<Boolean, C> createInstanceWithConstructor(
      Class<? extends C> clazz, Class[] params, Object[] values) {
    // Find constructor for params
    Constructor<? extends C> constructor;
    try {
      constructor = clazz.getConstructor(params);
    } catch (NoSuchMethodException e) {
      return new Pair<>(false, null);
    }

    // Invoke constructor using values as params
    C result;
    try {
      result = constructor.newInstance(values);
    } catch (Exception e) {
      return new Pair<>(false, null);
    }

    return new Pair<>(true, result);
  }

  /**
   * Creates instance of object using field -> value data
   *
   * @param clazz Object class
   * @param fieldValuePairs Field -> value info
   * @return Pair[success or not, created instance if success or null otherwise]
   */
  @Override
  public <C> Pair<Boolean, C> createObject(Class<? extends C> clazz,
      List<Pair<SSZField, Object>> fieldValuePairs) {
    Class[] params = new Class[fieldValuePairs.size()];
    for (int i = 0; i < fieldValuePairs.size(); i++) {
      Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object> pair = fieldValuePairs.get(i);
      SSZSchemeBuilder.SSZScheme.SSZField field = pair.getValue0();
//      switch (field.multipleType) {
//        case LIST:
//          {
//            params[i] = List.class;
//            break;
//          }
//        case ARRAY:
//          {
//            params[i] = Array.newInstance(field.fieldType, 0).getClass();
//            break;
//          }
//        default:
//          {
//            params[i] = field.fieldType;
//            break;
//          }
//      }
    }
    Object[] values = fieldValuePairs.stream().map(Pair::getValue1).toArray();

    return createInstanceWithConstructor(clazz, params, values);
  }
}
