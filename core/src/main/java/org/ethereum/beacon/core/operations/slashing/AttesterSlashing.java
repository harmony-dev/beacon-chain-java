package org.ethereum.beacon.core.operations.slashing;

import javax.annotation.Nullable;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class AttesterSlashing {
  @SSZ private final SlashableAttestation slashableAttestation1;
  @SSZ private final SlashableAttestation slashableAttestation2;

  public AttesterSlashing(
      SlashableAttestation slashableAttestation1,
      SlashableAttestation slashableAttestation2) {
    this.slashableAttestation1 = slashableAttestation1;
    this.slashableAttestation2 = slashableAttestation2;
  }

  public SlashableAttestation getSlashableAttestation1() {
    return slashableAttestation1;
  }

  public SlashableAttestation getSlashableAttestation2() {
    return slashableAttestation2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    AttesterSlashing that = (AttesterSlashing) o;
    if (!slashableAttestation1.equals(that.slashableAttestation1)) {return false;}
    return slashableAttestation2.equals(that.slashableAttestation2);
  }

  @Override
  public int hashCode() {
    int result = slashableAttestation1.hashCode();
    result = 31 * result + slashableAttestation2.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return toString(null, null);
  }

  public String toString(@Nullable SpecConstants spec,@Nullable Time beaconStart) {
    return "AttesterSlashing["
        + "att1=" + slashableAttestation1.toString(spec, beaconStart)
        + "att2=" + slashableAttestation2.toString(spec, beaconStart)
        + "]";
  }
}
