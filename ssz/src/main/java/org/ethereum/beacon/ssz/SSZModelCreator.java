package org.ethereum.beacon.ssz;

import org.javatuples.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Simple implementation of {@link SSZModelFactory}</p>
 * <p>Uses ObjectCreators to create object with defined fields and their values
 * one by one until success. If no success at all was achieved, throws {@link net.consensys.cava.ssz.SSZException}</p>
 */
public class SSZModelCreator implements SSZModelFactory {

  List<ObjectCreator> objectCreators = new ArrayList<>();

  /**
   * <p>Registers object creator which will be used for ssz model instantiation</p>
   * <p>First one registered has highest priority, if it failed, second is used, etc.</p>
   * @param objectCreator    Object creator
   * @return updated this
   */
  @Override
  public SSZModelFactory registerObjCreator(ObjectCreator objectCreator) {
    objectCreators.add(objectCreator);
    return this;
  }

  /**
   * <p>Creates instance of SSZ model class using registered object creators</p>
   * @param clazz             SSZ model class
   * @param fieldValuePairs   Field -> value info
   * @return created instance or {@link net.consensys.cava.ssz.SSZException} if failed to create it
   */
  public Object create(Class clazz, List<Pair<SSZSchemeBuilder.SSZScheme.SSZField, Object>> fieldValuePairs) {
    for (ObjectCreator objectCreator : objectCreators) {
      Pair<Boolean, Object> attempt = objectCreator.createObject(clazz, fieldValuePairs);
      if (!attempt.getValue0()) {
        continue;
      } else {
        return attempt.getValue1();
      }
    }

    // Throw error
    Object[] values = fieldValuePairs.stream()
        .map(Pair::getValue1)
        .toArray();
    String fieldTypes = Arrays.stream(values)
        .map(v -> v.getClass().toString())
        .collect(Collectors.joining(","));
    String error = String.format("Unable to find appropriate class %s "
        + "construction method with params [%s]."
        + "You should either have constructor with all non-transient fields "
        + "or setters/public fields.", clazz.getName(), fieldTypes);
    throw new SSZSchemeException(error);
  }
}
