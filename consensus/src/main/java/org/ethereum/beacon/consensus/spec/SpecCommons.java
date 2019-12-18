package org.ethereum.beacon.consensus.spec;

import java.util.function.Function;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
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

  boolean isVerifyDepositProof();

  boolean isComputableGenesisTime();

  default <T extends SpecAssertionFailed> void assertThat(boolean assertion, Class<T> type) {
    if (!assertion) {
      T exception;
      try {
        exception = type.newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      throw exception;
    }
  }

  default void assertTrue(boolean assertion) {
    assertThat(assertion, SpecAssertionFailed.class);
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
      for (int i = getStackTrace().length - 1; i >= 0; i--) {
        if (getStackTrace()[i].getClassName().equals(SpecCommons.class.getName())) {
          if (i + 1 < getStackTrace().length) {
            return String.format(
                "%s {%s}", this.getClass().getSimpleName(), getStackTrace()[i + 1].toString());
          }
          break;
        }
      }

      return "";
    }
  }
}
