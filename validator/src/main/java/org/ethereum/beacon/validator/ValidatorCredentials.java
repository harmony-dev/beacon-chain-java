package org.ethereum.beacon.validator;

import com.google.common.base.MoreObjects;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;

/** A list of attributes specific to certain validator instance. */
public class ValidatorCredentials {

  private final Bytes48 blsPublicKey;
  private final Bytes32 withdrawalCredentials;

  public ValidatorCredentials(Bytes48 blsPublicKey, Bytes32 withdrawalCredentials) {
    this.blsPublicKey = blsPublicKey;
    this.withdrawalCredentials = withdrawalCredentials;
  }

  public Bytes48 getBlsPublicKey() {
    return blsPublicKey;
  }

  public Bytes32 getWithdrawalCredentials() {
    return withdrawalCredentials;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("blsPublicKey", blsPublicKey)
        .add("withdrawalCredentials", withdrawalCredentials)
        .toString();
  }
}
