package org.ethereum.beacon.core.operations.slashing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;

/**
 * Slashable attestation data structure.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.5.0/specs/core/0_beacon-chain.md#slashableattestation">SlashableAttestation</a>
 *     in the spec.
 */
@SSZSerializable
public class SlashableAttestation {
  /** Validator indices */
  @SSZ private final List<ValidatorIndex> validatorIndicesList;
  /** Attestation data */
  @SSZ private final AttestationData data;
  /** Custody bitfield */
  @SSZ private final Bitfield custodyBitfield;
  /** Aggregate signature */
  @SSZ private final BLSSignature aggregateSingature;

  public SlashableAttestation(
      List<ValidatorIndex> validatorIndices,
      AttestationData data, Bitfield custodyBitfield,
      BLSSignature aggregateSingature) {
    this.validatorIndicesList = new ArrayList<>(validatorIndices);
    this.data = data;
    this.custodyBitfield = custodyBitfield;
    this.aggregateSingature = aggregateSingature;
  }

  public ReadList<Integer, ValidatorIndex> getValidatorIndices() {
    return WriteList.wrap(validatorIndicesList, Integer::valueOf);
  }

  public AttestationData getData() {
    return data;
  }

  public Bitfield getCustodyBitfield() {
    return custodyBitfield;
  }

  public BLSSignature getAggregateSingature() {
    return aggregateSingature;
  }

  /**
   * @deprecated for serialization only
   */
  public List<ValidatorIndex> getValidatorIndicesList() {
    return validatorIndicesList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    SlashableAttestation that = (SlashableAttestation) o;
    if (!validatorIndicesList.equals(that.validatorIndicesList)) {return false;}
    if (!data.equals(that.data)) {return false;}
    if (!custodyBitfield.equals(that.custodyBitfield)) {return false;}
    return aggregateSingature.equals(that.aggregateSingature);
  }

  @Override
  public int hashCode() {
    int result = validatorIndicesList.hashCode();
    result = 31 * result + data.hashCode();
    result = 31 * result + custodyBitfield.hashCode();
    result = 31 * result + aggregateSingature.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,@Nullable Time beaconStart) {
    return "SlashableAttestation["
        + "data=" + data.toString(spec, beaconStart)
        + ", validators=" + validatorIndicesList
        + ", custodyBits=" + custodyBitfield
        + ", sig=" + aggregateSingature
        + "]";
  }
}
