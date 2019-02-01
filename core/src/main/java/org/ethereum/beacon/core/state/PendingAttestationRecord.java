package org.ethereum.beacon.core.state;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.BytesValue;

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

  /** Proof of custody bitfield. */
  @SSZ private final BytesValue aggregationBitfield;
  /** Signed data. */
  @SSZ private final AttestationData data;
  /** Attester participation bitfield. */
  @SSZ private final BytesValue custodyBitfield;
  /** Slot in which it was included. */
  @SSZ private final SlotNumber inclusionSlot;

  public PendingAttestationRecord(BytesValue aggregationBitfield,
      AttestationData data, BytesValue custodyBitfield,
      SlotNumber inclusionSlot) {
    this.aggregationBitfield = aggregationBitfield;
    this.data = data;
    this.custodyBitfield = custodyBitfield;
    this.inclusionSlot = inclusionSlot;
  }

  public BytesValue getAggregationBitfield() {
    return aggregationBitfield;
  }

  public AttestationData getData() {
    return data;
  }

  public BytesValue getCustodyBitfield() {
    return custodyBitfield;
  }

  public SlotNumber getInclusionSlot() {
    return inclusionSlot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PendingAttestationRecord that = (PendingAttestationRecord) o;
    return Objects.equal(data, that.data)
        && Objects.equal(aggregationBitfield, that.aggregationBitfield)
        && Objects.equal(custodyBitfield, that.custodyBitfield)
        && Objects.equal(inclusionSlot, that.inclusionSlot);
  }
}
