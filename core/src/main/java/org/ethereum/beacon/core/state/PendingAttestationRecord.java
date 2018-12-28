package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
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
public class PendingAttestationRecord {

  /** Signed data. */
  private final AttestationData data;
  /** Attester participation bitfield. */
  private final BytesValue participationBitfield;
  /** Proof of custody bitfield. */
  private final BytesValue custodyBitfield;
  /** Slot in which it was included. */
  private final UInt64 slotIncluded;

  public PendingAttestationRecord(
      AttestationData data,
      BytesValue participationBitfield,
      BytesValue custodyBitfield,
      UInt64 slotIncluded) {
    this.data = data;
    this.participationBitfield = participationBitfield;
    this.custodyBitfield = custodyBitfield;
    this.slotIncluded = slotIncluded;
  }

  public AttestationData getData() {
    return data;
  }

  public BytesValue getParticipationBitfield() {
    return participationBitfield;
  }

  public BytesValue getCustodyBitfield() {
    return custodyBitfield;
  }

  public UInt64 getSlotIncluded() {
    return slotIncluded;
  }
}
