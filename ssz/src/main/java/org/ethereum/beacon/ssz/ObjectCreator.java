package org.ethereum.beacon.ssz;

import org.javatuples.Pair;
import java.util.List;

/** Creates instance of object using input and method which depends on implementation */
public interface ObjectCreator {
  /**
   * Creates instance of object using field -> value data
   *
   * @param clazz Object class
   * @param fieldValuePairs Field -> value info
   * @return Pair[success or not, created instance if success or null otherwise]
   */
  Pair<Boolean, Object> createObject(
      Class clazz, List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs);
}
