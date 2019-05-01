package org.ethereum.beacon.core.operations.slashing;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.collections.ReadList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Slashable attestation data structure.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.6.0/specs/core/0_beacon-chain.md#indexedattestation">IndexedAttestation</a>
 *     in the spec.
 */
@SSZSerializable
public class IndexedAttestation {
  /** Validator indices */
  @SSZ private final ReadList<Integer, ValidatorIndex> custodyBit0Indices;

  @SSZ private final ReadList<Integer, ValidatorIndex> custodyBit1Indices;
  /** Attestation data */
  @SSZ private final AttestationData data;
  /** Aggregate signature */
  @SSZ private final BLSSignature signature;

  public IndexedAttestation(
      List<ValidatorIndex> custodyBit0Indices,
      List<ValidatorIndex> custodyBit1Indices,
      AttestationData data,
      BLSSignature signature) {
    this(
        ReadList.wrap(custodyBit0Indices, Function.identity()),
        ReadList.wrap(custodyBit1Indices, Function.identity()),
        data,
        signature);
  }

  public IndexedAttestation(
      ReadList<Integer, ValidatorIndex> custodyBit0Indices,
      ReadList<Integer, ValidatorIndex> custodyBit1Indices,
      AttestationData data,
      BLSSignature signature) {
    this.custodyBit0Indices = custodyBit0Indices;
    this.custodyBit1Indices = custodyBit1Indices;
    this.data = data;
    this.signature = signature;
  }

  public ReadList<Integer, ValidatorIndex> getCustodyBit0Indices() {
    return custodyBit0Indices;
  }

  public ReadList<Integer, ValidatorIndex> getCustodyBit1Indices() {
    return custodyBit1Indices;
  }

  public AttestationData getData() {
    return data;
  }

  public BLSSignature getSignature() {
    return signature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IndexedAttestation that = (IndexedAttestation) o;
    return Objects.equal(custodyBit0Indices, that.custodyBit0Indices)
        && Objects.equal(custodyBit1Indices, that.custodyBit1Indices)
        && Objects.equal(data, that.data)
        && Objects.equal(signature, that.signature);
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec, @Nullable Time beaconStart) {
    return "IndexedAttestation["
        + "data="
        + data.toString(spec, beaconStart)
        + ", custodyBit0Indices="
        + custodyBit0Indices
        + ", custodyBit1Indices="
        + custodyBit1Indices
        + ", sig="
        + signature
        + "]";
  }
}
