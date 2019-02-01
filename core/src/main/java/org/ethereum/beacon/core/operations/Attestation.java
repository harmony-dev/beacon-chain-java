package org.ethereum.beacon.core.operations;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
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
@SSZSerializable
public class Attestation {

  /** Attestation data object. */
  @SSZ private final AttestationData data;
  /** A bitfield where each bit corresponds to a validator attested to the {@link #data}. */
  @SSZ private final BytesValue aggregationBitfield;
  /** Proof of custody bitfield. */
  @SSZ private final BytesValue custodyBitfield;
  /** A product of aggregation of signatures from different validators to {@link #data}. */
  @SSZ private final BLSSignature aggregateSignature;

  public Attestation(
      AttestationData data,
      BytesValue aggregationBitfield,
      BytesValue custodyBitfield,
      BLSSignature aggregateSignature) {
    this.data = data;
    this.aggregationBitfield = aggregationBitfield;
    this.custodyBitfield = custodyBitfield;
    this.aggregateSignature = aggregateSignature;
  }

  public AttestationData getData() {
    return data;
  }

  public BytesValue getAggregationBitfield() {
    return aggregationBitfield;
  }

  public BytesValue getCustodyBitfield() {
    return custodyBitfield;
  }

  public BLSSignature getAggregateSignature() {
    return aggregateSignature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Attestation that = (Attestation) o;
    return Objects.equal(data, that.data)
        && Objects.equal(aggregationBitfield, that.aggregationBitfield)
        && Objects.equal(custodyBitfield, that.custodyBitfield)
        && Objects.equal(aggregateSignature, that.aggregateSignature);
  }
}
