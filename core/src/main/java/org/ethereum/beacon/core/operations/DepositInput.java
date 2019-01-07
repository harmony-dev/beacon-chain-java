package org.ethereum.beacon.core.operations;

import org.ethereum.beacon.util.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;

@SSZSerializable
public class DepositInput {
  private final Bytes48 pubKey;
  private final Hash32 withdrawalCredentials;
  private final Hash32 randaoCommitment;
  private final Hash32 pocCommitment;
  private final Bytes96 proofOfPossession;

  public DepositInput(
      Bytes48 pubKey,
      Hash32 withdrawalCredentials,
      Hash32 randaoCommitment,
      Hash32 pocCommitment,
      Bytes96 proofOfPossession) {
    this.pubKey = pubKey;
    this.withdrawalCredentials = withdrawalCredentials;
    this.randaoCommitment = randaoCommitment;
    this.pocCommitment = pocCommitment;
    this.proofOfPossession = proofOfPossession;
  }

  public Bytes48 getPubKey() {
    return pubKey;
  }

  public Hash32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  public Hash32 getRandaoCommitment() {
    return randaoCommitment;
  }

  public Hash32 getPocCommitment() {
    return pocCommitment;
  }

  public Bytes96 getProofOfPossession() {
    return proofOfPossession;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DepositInput that = (DepositInput) o;
    return pubKey.equals(that.pubKey) &&
        withdrawalCredentials.equals(that.withdrawalCredentials) &&
        randaoCommitment.equals(that.randaoCommitment) &&
        pocCommitment.equals(that.pocCommitment) &&
        proofOfPossession.equals(that.proofOfPossession);
  }
}
