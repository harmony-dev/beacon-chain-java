package org.ethereum.beacon.core;

import com.google.common.base.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Hashable;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Beacon block header structure.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#beaconblockheader">BeaconBlockHeader</a>
 *     in the spec.
 */
@SSZSerializable
public class BeaconBlockHeader implements Hashable<Hash32> {

  public static final BeaconBlockHeader EMPTY =
      new BeaconBlockHeader(
          SlotNumber.ZERO, Hash32.ZERO, Hash32.ZERO, Hash32.ZERO, BLSSignature.ZERO);

  @SSZ private final SlotNumber slot;
  @SSZ private final Hash32 previousBlockRoot;
  @SSZ private final Hash32 stateRoot;
  @SSZ private final Hash32 blockBodyRoot;
  @SSZ private final BLSSignature signature;

  private Hash32 hashCache = null;

  public BeaconBlockHeader(
      SlotNumber slot,
      Hash32 previousBlockRoot,
      Hash32 stateRoot,
      Hash32 blockBodyRoot,
      BLSSignature signature) {
    this.slot = slot;
    this.previousBlockRoot = previousBlockRoot;
    this.stateRoot = stateRoot;
    this.blockBodyRoot = blockBodyRoot;
    this.signature = signature;
  }

  public SlotNumber getSlot() {
    return slot;
  }

  public Hash32 getPreviousBlockRoot() {
    return previousBlockRoot;
  }

  public Hash32 getStateRoot() {
    return stateRoot;
  }

  public Hash32 getBlockBodyRoot() {
    return blockBodyRoot;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  public BeaconBlockHeader withStateRoot(Hash32 stateRoot) {
    return new BeaconBlockHeader(slot, previousBlockRoot, stateRoot, blockBodyRoot, signature);
  }

  @Override
  public Optional<Hash32> getHash() {
    return Optional.ofNullable(hashCache);
  }

  @Override
  public void setHash(Hash32 hash) {
    this.hashCache = hash;
  }
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    BeaconBlockHeader that = (BeaconBlockHeader) object;
    return Objects.equal(slot, that.slot)
        && Objects.equal(previousBlockRoot, that.previousBlockRoot)
        && Objects.equal(stateRoot, that.stateRoot)
        && Objects.equal(blockBodyRoot, that.blockBodyRoot)
        && Objects.equal(signature, that.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(slot, previousBlockRoot, stateRoot, blockBodyRoot, signature);
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toStringFull(
      @Nullable SpecConstants constants,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    return "BlockHeader[" + toStringPriv(constants, hasher) + "]:\n";
  }

  public String toString(@Nullable SpecConstants constants,
      @Nullable Function<? super BeaconBlockHeader, Hash32> hasher) {
    return (hasher == null ? "?" : hasher.apply(this).toStringShort())
        + " <~ " + previousBlockRoot.toStringShort();
  }

  private String toStringPriv(@Nullable SpecConstants constants,
      @Nullable Function<? super Hashable<Hash32>, Hash32> hasher) {
    return (hasher == null ? "?" : hasher.apply(this).toStringShort())
        + " <~ " + previousBlockRoot.toStringShort()
        + ", @slot " + slot.toStringNumber(constants)
        + ", state=" + stateRoot.toStringShort()
        + ", body=" + blockBodyRoot.toStringShort()
        + ", sig=" + signature;
  }
}
