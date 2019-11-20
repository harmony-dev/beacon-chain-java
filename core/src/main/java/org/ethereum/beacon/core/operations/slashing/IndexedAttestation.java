package org.ethereum.beacon.core.operations.slashing;

import com.google.common.base.Objects;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.collections.ReadList;

import java.util.List;
import java.util.function.Function;

/**
 * Slashable attestation data structure.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.1/specs/core/0_beacon-chain.md#indexedattestation">IndexedAttestation</a>
 *     in the spec.
 */
@SSZSerializable
public class IndexedAttestation {
  /** Attesting indices. */
  @SSZ(maxSizeVar = "spec.MAX_VALIDATORS_PER_COMMITTEE")
  private final ReadList<Integer, ValidatorIndex> attestingIndices;
  /** Attestation data */
  @SSZ private final AttestationData data;
  /** Aggregate signature */
  @SSZ private final BLSSignature signature;

  public IndexedAttestation(
      List<ValidatorIndex> attestingIndices,
      AttestationData data,
      BLSSignature signature,
      SpecConstants specConstants) {
    this(
        ReadList.wrap(
            attestingIndices,
            Function.identity(),
            specConstants.getMaxValidatorsPerCommittee().longValue()),
        data,
        signature);
  }

  public IndexedAttestation(
      ReadList<Integer, ValidatorIndex> attestingIndices,
      AttestationData data,
      BLSSignature signature,
      SpecConstants specConstants) {
    this(
        attestingIndices.maxSize() == ReadList.VARIABLE_SIZE
            ? attestingIndices.cappedCopy(
                specConstants.getMaxValidatorsPerCommittee().longValue())
            : attestingIndices,
        data,
        signature);
  }

  private IndexedAttestation(
      ReadList<Integer, ValidatorIndex> attestingIndices,
      AttestationData data,
      BLSSignature signature) {
    this.attestingIndices = attestingIndices;
    this.data = data;
    this.signature = signature;
  }

  public ReadList<Integer, ValidatorIndex> getAttestingIndices() {
    return attestingIndices;
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
    return Objects.equal(attestingIndices, that.attestingIndices)
        && Objects.equal(data, that.data)
        && Objects.equal(signature, that.signature);
  }

  @Override
  public int hashCode() {
    int result = attestingIndices.hashCode();
    result = 31 * result + data.hashCode();
    result = 31 * result + signature.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "IndexedAttestation["
        + "data="
        + data.toString()
        + ", attestingIndices="
        + attestingIndices
        + ", sig="
        + signature
        + "]";
  }
}
