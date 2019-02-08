package org.ethereum.beacon.core.operations.slashing;

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
}
