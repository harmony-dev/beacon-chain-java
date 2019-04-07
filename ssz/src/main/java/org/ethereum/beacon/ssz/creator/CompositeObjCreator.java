package org.ethereum.beacon.ssz.creator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.javatuples.Pair;
import java.util.List;

/** Creates instance of SSZ model class */
public class CompositeObjCreator implements ObjectCreator {

  private List<ObjectCreator> objectCreators = new ArrayList<>();

  public CompositeObjCreator(List<ObjectCreator> creators) {
    this.objectCreators = new ArrayList<>(creators);
  }

  public CompositeObjCreator(ObjectCreator... creators) {
    this(Arrays.asList(creators));
  }


  /**
   * Registers object creator which will be used for ssz model instantiation
   *
   * <p>First one registered has highest priority, if it failed, second is used, etc.
   *
   * @param objectCreator Object creator
   * @return updated this
   */
  public CompositeObjCreator registerObjCreator(ObjectCreator objectCreator) {
    objectCreators.add(objectCreator);
    return this;
  }

  /**
   * Creates instance of SSZ model class using registered object creators
   *
   * @param clazz SSZ model class
   * @param fieldValuePairs Field -> value info
   * @return created instance or {@link net.consensys.cava.ssz.SSZException} if failed to create it
   */
  @Override
  public <C> C createObject(Class<? extends C> clazz, List<Pair<SSZField, Object>> fieldValuePairs) {
    for (ObjectCreator objectCreator : objectCreators) {
      C attempt = objectCreator.createObject(clazz, fieldValuePairs);
      if (attempt != null) {
        return attempt;
      }
    }

    // Throw error
    Object[] values = fieldValuePairs.stream().map(Pair::getValue1).toArray();
    String fieldTypes =
        Arrays.stream(values).map(v -> v.getClass().toString()).collect(Collectors.joining(","));
    String error =
        String.format(
            "Unable to find appropriate class %s "
                + "construction method with params [%s]."
                + "You should either have constructor with all non-transient fields "
                + "or setters/public fields.",
            clazz.getName(), fieldTypes);
    throw new SSZSchemeException(error);
  }
}