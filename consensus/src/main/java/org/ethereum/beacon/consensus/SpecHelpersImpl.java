package org.ethereum.beacon.consensus;

import java.util.function.Function;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstants;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Default implementation of {@link SpecHelpers}. */
public class SpecHelpersImpl implements SpecHelpers {
  private final SpecConstants constants;
  private final ObjectHasher<Hash32> objectHasher;
  private final Function<BytesValue, Hash32> hashFunction;

  public SpecHelpersImpl(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher) {
    this.constants = constants;
    this.objectHasher = objectHasher;
    this.hashFunction = hashFunction;
  }

  @Override
  public SpecConstants getConstants() {
    return constants;
  }

  @Override
  public ObjectHasher<Hash32> getObjectHasher() {
    return objectHasher;
  }

  @Override
  public Function<BytesValue, Hash32> getHashFunction() {
    return hashFunction;
  }
}
