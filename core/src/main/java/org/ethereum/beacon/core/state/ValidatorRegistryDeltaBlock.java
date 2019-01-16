package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.Hashable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * A diff between two changes of the validator registry.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#validatorregistrydeltablock>ValidatorRegistryDeltaBlock
 *     in the spec</a>
 */
public class ValidatorRegistryDeltaBlock implements Hashable {

  /** A hash of previous registry delta block. */
  private final Hash32 latestRegistryDeltaRoot;
  /** An index of validator that has been changed. */
  private final UInt24 validatorIndex;
  /** BLS public key of the validator. */
  private final Bytes48 pubKey;

  private final UInt64 slot;
  /** A code denoting an action applied to the validator. */
  private final UInt64 flag;

  public ValidatorRegistryDeltaBlock(Hash32 latestRegistryDeltaRoot, UInt24 validatorIndex,
      Bytes48 pubKey, UInt64 slot, UInt64 flag) {
    this.latestRegistryDeltaRoot = latestRegistryDeltaRoot;
    this.validatorIndex = validatorIndex;
    this.pubKey = pubKey;
    this.slot = slot;
    this.flag = flag;
  }

  public Hash32 getLatestRegistryDeltaRoot() {
    return latestRegistryDeltaRoot;
  }

  public UInt24 getValidatorIndex() {
    return validatorIndex;
  }

  public Bytes48 getPubKey() {
    return pubKey;
  }

  public UInt64 getSlot() {
    return slot;
  }

  public UInt64 getFlag() {
    return flag;
  }

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }
}
