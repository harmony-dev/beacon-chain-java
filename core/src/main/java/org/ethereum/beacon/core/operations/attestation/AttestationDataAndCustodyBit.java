package org.ethereum.beacon.core.operations.attestation;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/**
 * Attestation data plus custody bit.
 *
 * @see AttestationData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#attestationdataandcustodybit">AttestationDataAndCustodyBit</a>
 *     in the spec.
 */
@SSZSerializable
public class AttestationDataAndCustodyBit {

  /** Attestation data. */
  @SSZ private final AttestationData data;
  /** Challengeable bit (SSZ-bool, 1 byte) for the custody of crosslink data. */
  @SSZ private final boolean custodyBit;

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
