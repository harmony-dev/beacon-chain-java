package org.ethereum.beacon.core.operations.attestation;

/**
 * Attestation data plus custody bit.
 *
 * @see AttestationData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#attestationdataandcustodybit">AttestationDataAndCustodyBit</a>
 *     in the spec.
 */
public class AttestationDataAndCustodyBit {

  /** Attestation data. */
  private final AttestationData data;
  /** Custody bit. */
  private final boolean custodyBit;

  public AttestationDataAndCustodyBit(AttestationData data, boolean custodyBit) {
    this.data = data;
    this.custodyBit = custodyBit;
  }

  public AttestationData getData() {
    return data;
  }

  public boolean isCustodyBit() {
    return custodyBit;
  }
}
