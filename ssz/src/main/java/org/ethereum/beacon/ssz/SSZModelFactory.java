package org.ethereum.beacon.ssz;

import org.javatuples.Pair;
import java.util.List;

/**
 * Creates instance of SSZ model class
 */
public interface SSZModelFactory {
  /**
   * Creates instance of SSZ model class using field -> value data
   * @param clazz             SSZ model class
   * @param fieldValuePairs   Field -> value info
   * @return created instance
   */
  Object create(Class clazz, List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs);
}
