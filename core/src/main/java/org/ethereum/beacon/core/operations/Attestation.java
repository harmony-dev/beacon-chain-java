package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Attests on a block linked to particular slot in particular shard.
 *
 * @see BeaconBlockBody
 * @see AttestationData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#attestation">Attestation
 *     in the spec</a>
 */
public class Attestation {

  /** Attestation data object. */
  private final AttestationData data;
  /** A bitfield where each bit corresponds to a validator attested to the {@link #data}. */
  private final BytesValue participationBitfield;
  /** Proof of custody bitfield. */
  private final BytesValue custodyBitfield;
  /** A product of aggregation of signatures from different validators to {@link #data}. */
  private final Bytes96 aggregatedSignature;

  public Attestation(
      AttestationData data,
      BytesValue participationBitfield,
      BytesValue custodyBitfield,
      Bytes96 aggregatedSignature) {
    this.data = data;
    this.participationBitfield = participationBitfield;
    this.custodyBitfield = custodyBitfield;
    this.aggregatedSignature = aggregatedSignature;
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

  public Bytes96 getAggregatedSignature() {
    return aggregatedSignature;
  }
}
