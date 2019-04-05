package org.ethereum.beacon.ssz.creator;

import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
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
  <C> C createObject(
      Class<? extends C> clazz, List<Pair<SSZField, Object>> fieldValuePairs);
}
