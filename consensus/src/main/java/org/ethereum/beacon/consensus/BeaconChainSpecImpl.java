package org.ethereum.beacon.consensus;

import java.util.function.Function;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstants;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Default implementation of {@link BeaconChainSpec}. */
public class BeaconChainSpecImpl implements BeaconChainSpec {
  private final SpecConstants constants;
  private final Function<BytesValue, Hash32> hashFunction;
  private final ObjectHasher<Hash32> objectHasher;
  private final boolean blsVerify;
  private final boolean blsVerifyProofOfPossession;
  private final boolean verifyDepositProof;

  public BeaconChainSpecImpl(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher,
      boolean blsVerify,
      boolean blsVerifyProofOfPossession,
      boolean verifyDepositProof) {
    this.constants = constants;
    this.hashFunction = hashFunction;
    this.objectHasher = objectHasher;
    this.blsVerify = blsVerify;
    this.blsVerifyProofOfPossession = blsVerifyProofOfPossession;
    this.verifyDepositProof = verifyDepositProof;
  }

  @Override
  public SpecConstants getConstants() {
    return constants;
  }

  @Override
  public ObjectHasher<Hash32> getObjectHasher() {
    return objectHasher;
  }

  @Override
  public Function<BytesValue, Hash32> getHashFunction() {
    return hashFunction;
  }

  @Override
  public boolean isBlsVerify() {
    return blsVerify;
  }

  @Override
  public boolean isBlsVerifyProofOfPossession() {
    return blsVerifyProofOfPossession;
  }

  @Override
  public boolean isVerifyDepositProof() {
    return verifyDepositProof;
  }
}
