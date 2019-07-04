package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

/**
 * A common part of the spec that is shared by all its components.
 */
public interface SpecCommons {

  SpecConstants getConstants();

  ObjectHasher<Hash32> getObjectHasher();

  Function<BytesValue, Hash32> getHashFunction();

  boolean isBlsVerify();

  boolean isBlsVerifyProofOfPossession();

  default void assertTrue(boolean assertion) {
    if (!assertion) {
      throw new SpecAssertionFailed();
    }
  }

  default void checkIndexRange(BeaconState state, ValidatorIndex index) {
    assertTrue(index.less(state.getValidators().size()));
  }

  class SpecAssertionFailed extends RuntimeException {
    @Override
    public String getMessage() {
      return toString();
    }

    @Override
    public String toString() {
      return String.format(
          "SpecAssertionFailed{%s}",
          getStackTrace().length > 1 ? getStackTrace()[1].toString() : "");
    }
  }
}
