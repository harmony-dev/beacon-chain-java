package org.ethereum.beacon.core.operations.slashing;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;

@SSZSerializable
public class SlashableAttestation {
  @SSZ private final List<ValidatorIndex> validatorIndicesList;
  @SSZ private final AttestationData data;
  @SSZ private final Bitfield custodyBitfield;
  @SSZ private final BLSSignature blsSignature;

  public SlashableAttestation(
      List<ValidatorIndex> validatorIndices,
      AttestationData data, Bitfield custodyBitfield,
      BLSSignature blsSignature) {
    this.validatorIndicesList = new ArrayList<>(validatorIndices);
    this.data = data;
    this.custodyBitfield = custodyBitfield;
    this.blsSignature = blsSignature;
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

  public BLSSignature getBlsSignature() {
    return blsSignature;
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
    return blsSignature.equals(that.blsSignature);
  }
}
