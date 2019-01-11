package org.ethereum.beacon.ssz;

import org.javatuples.Pair;
import java.util.List;

public interface SSZModelFactory {
  Object create(Class clazz, List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs);
}
