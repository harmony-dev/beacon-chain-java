package org.ethereum.beacon.consensus.spec;

import java.util.function.Function;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

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
    assertTrue(index.less(state.getValidatorRegistry().size()));
  }

  default void checkIndexRange(BeaconState state, Iterable<ValidatorIndex> indices) {
    indices.forEach(index -> checkIndexRange(state, index));
  }

  default void checkShardRange(ShardNumber shard) {
    assertTrue(shard.less(getConstants().getShardCount()));
  }

  class SpecAssertionFailed extends RuntimeException {}
}
