package org.ethereum.beacon.core.operations.slashing;

import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import java.util.Arrays;

@SSZSerializable
/**
 * Data for Casper slashing operation.
 *
 * @see CasperSlashing
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#slashablevotedata">SlashableVoteData
 *     in the spec</a>
 */
public class SlashableVoteData {

  /** Proof-of-custody indices (0 bits). */
  private final UInt24[] custodyBit0Indices;
  /** Proof-of-custody indices (1 bits). */
  private final UInt24[] custodyBit1Indices;
  /** Attestation data. */
  private final AttestationData data;
  /** Aggregated signature. */
  private final Bytes96 aggregatedSignature;

  public SlashableVoteData(
      UInt24[] custodyBit0Indices,
      UInt24[] custodyBit1Indices,
      AttestationData data,
      Bytes96 aggregatedSignature) {
    this.custodyBit0Indices = custodyBit0Indices;
    this.custodyBit1Indices = custodyBit1Indices;
    this.data = data;
    this.aggregatedSignature = aggregatedSignature;
  }

  public UInt24[] getCustodyBit0Indices() {
    return custodyBit0Indices;
  }

  public UInt24[] getCustodyBit1Indices() {
    return custodyBit1Indices;
  }

  public AttestationData getData() {
    return data;
  }

  public Bytes96 getAggregatedSignature() {
    return aggregatedSignature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SlashableVoteData that = (SlashableVoteData) o;
    return Arrays.equals(custodyBit0Indices, that.custodyBit0Indices) &&
        Arrays.equals(custodyBit1Indices, that.custodyBit1Indices) &&
        data.equals(that.data) &&
        aggregatedSignature.equals(that.aggregatedSignature);
  }
}
