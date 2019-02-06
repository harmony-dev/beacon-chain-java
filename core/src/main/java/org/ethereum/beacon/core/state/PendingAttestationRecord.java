package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * An attestation data that have not been processed yet.
 *
 * @see BeaconState
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#pendingattestationrecord">PendingAttestationRecord
 *     in the spec</a>
 */
@SSZSerializable
public class PendingAttestationRecord {

  /** Signed data. */
  @SSZ private final AttestationData data;
  /** Attester participation bitfield. */
  @SSZ private final Bitfield participationBitfield;
  /** Proof of custody bitfield. */
  @SSZ private final Bitfield custodyBitfield;
  /** Slot in which it was included. */
  @SSZ private final SlotNumber slotIncluded;

  public PendingAttestationRecord(
      AttestationData data,
      Bitfield participationBitfield,
      Bitfield custodyBitfield,
      SlotNumber slotIncluded) {
    this.data = data;
    this.participationBitfield = participationBitfield;
    this.custodyBitfield = custodyBitfield;
    this.slotIncluded = slotIncluded;
  }

  public AttestationData getData() {
    return data;
  }

  public Bitfield getParticipationBitfield() {
    return participationBitfield;
  }

  public Bitfield getCustodyBitfield() {
    return custodyBitfield;
  }

  public SlotNumber getSlotIncluded() {
    return slotIncluded;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PendingAttestationRecord that = (PendingAttestationRecord) o;
    return Objects.equal(data, that.data)
        && Objects.equal(participationBitfield, that.participationBitfield)
        && Objects.equal(custodyBitfield, that.custodyBitfield)
        && Objects.equal(slotIncluded, that.slotIncluded);
  }
}
