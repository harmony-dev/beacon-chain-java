package org.ethereum.beacon.core;

import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.Objects;

@SSZSerializable
public class BeaconBlock implements Hashable {

  private final UInt64 slot;
  private final Hash32 parentRoot;
  private final Hash32 stateRoot;
  private final Hash32 randaoReveal;
  private final Hash32 candidatePowReceiptRoot;
  private final Bytes96 signature;
  private final BeaconBlockBody body;

  BeaconBlock(
      UInt64 slot,
      Hash32 parentRoot,
      Hash32 stateRoot,
      Hash32 randaoReveal,
      Hash32 candidatePowReceiptRoot,
      Bytes96 signature,
      BeaconBlockBody body) {
    this.slot = slot;
    this.parentRoot = parentRoot;
    this.stateRoot = stateRoot;
    this.randaoReveal = randaoReveal;
    this.candidatePowReceiptRoot = candidatePowReceiptRoot;
    this.signature = signature;
    this.body = body;
  }

  public Hash32 getStateRoot() {
    return stateRoot;
  }

  @Override
  public Hash32 getHash() {
    return Hash32.ZERO;
  }

  public Hash32 getParentRoot() {
    return Hash32.ZERO;
  }

  public boolean isParentOf(BeaconBlock other) {
    return true;
  }

  public BeaconBlock withStateRoot(Hash32 stateRoot) {
    return new BeaconBlock(
        slot, parentRoot, stateRoot, randaoReveal, candidatePowReceiptRoot, signature, body);
  }

  public UInt64 getSlot() {
    return slot;
  }

  public Hash32 getRandaoReveal() {
    return randaoReveal;
  }

  public Hash32 getCandidatePowReceiptRoot() {
    return candidatePowReceiptRoot;
  }

  public Bytes96 getSignature() {
    return signature;
  }

  public BeaconBlockBody getBody() {
    return body;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BeaconBlock that = (BeaconBlock) o;
    return slot.equals(that.slot) &&
        parentRoot.equals(that.parentRoot) &&
        stateRoot.equals(that.stateRoot) &&
        randaoReveal.equals(that.randaoReveal) &&
        candidatePowReceiptRoot.equals(that.candidatePowReceiptRoot) &&
        signature.equals(that.signature) &&
        body.equals(that.body);
  }
}
