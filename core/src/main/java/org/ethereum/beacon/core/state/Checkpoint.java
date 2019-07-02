package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Checkpoint type.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#checkpoint">Checkpoint</a>
 *     in the spec.
 */
public class Checkpoint {

  public static final Checkpoint EMPTY = new Checkpoint(EpochNumber.ZERO, Hash32.ZERO);

  @SSZ private final EpochNumber epoch;
  @SSZ private final Hash32 root;

  public Checkpoint(EpochNumber epoch, Hash32 root) {
    this.epoch = epoch;
    this.root = root;
  }

  public EpochNumber getEpoch() {
    return epoch;
  }

  public Hash32 getRoot() {
    return root;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Checkpoint that = (Checkpoint) o;
    return Objects.equal(epoch, that.epoch) && Objects.equal(root, that.root);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(epoch, root);
  }
}
