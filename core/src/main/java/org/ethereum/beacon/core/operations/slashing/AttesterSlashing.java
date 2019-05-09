package org.ethereum.beacon.core.operations.slashing;

import javax.annotation.Nullable;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class AttesterSlashing {
  @SSZ private final IndexedAttestation attestation1;
  @SSZ private final IndexedAttestation attestation2;

  public AttesterSlashing(
      IndexedAttestation attestation1,
      IndexedAttestation attestation2) {
    this.attestation1 = attestation1;
    this.attestation2 = attestation2;
  }

  public IndexedAttestation getAttestation1() {
    return attestation1;
  }

  public IndexedAttestation getAttestation2() {
    return attestation2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    AttesterSlashing that = (AttesterSlashing) o;
    if (!attestation1.equals(that.attestation1)) {return false;}
    return attestation2.equals(that.attestation2);
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,@Nullable Time beaconStart) {
    return "AttesterSlashing["
        + "att1=" + attestation1.toString(spec, beaconStart)
        + "att2=" + attestation2.toString(spec, beaconStart)
        + "]";
  }
}
