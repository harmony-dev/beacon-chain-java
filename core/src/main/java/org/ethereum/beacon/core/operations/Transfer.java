package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/**
 * A value transfer between validator.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/0.4.0/specs/core/0_beacon-chain.md#transfers">Transfers</a>
 *     in the spec.
 */
@SSZSerializable
public class Transfer {
  /** Sender index. */
  @SSZ private final ValidatorIndex from;
  /** Recipient index. */
  @SSZ private final ValidatorIndex to;
  /** Amount in Gwei. */
  @SSZ private final Gwei amount;
  /** Fee in Gwei for block proposer. */
  @SSZ private final Gwei fee;
  /** Inclusion slot. */
  @SSZ private final SlotNumber slot;
  /** Sender withdrawal pubkey. */
  @SSZ private final BLSPubkey pubkey;
  /** Sender signature. */
  @SSZ private final BLSSignature signature;

  public Transfer(
      ValidatorIndex from,
      ValidatorIndex to,
      Gwei amount,
      Gwei fee,
      SlotNumber slot,
      BLSPubkey pubkey,
      BLSSignature signature) {
    this.from = from;
    this.to = to;
    this.amount = amount;
    this.fee = fee;
    this.slot = slot;
    this.pubkey = pubkey;
    this.signature = signature;
  }

  public ValidatorIndex getFrom() {
    return from;
  }

  public ValidatorIndex getTo() {
    return to;
  }

  public Gwei getAmount() {
    return amount;
  }

  public Gwei getFee() {
    return fee;
  }

  public SlotNumber getSlot() {
    return slot;
  }

  public BLSPubkey getPubkey() {
    return pubkey;
  }

  public BLSSignature getSignature() {
    return signature;
  }
}
