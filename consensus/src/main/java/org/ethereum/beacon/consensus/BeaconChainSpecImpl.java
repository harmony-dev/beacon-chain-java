package org.ethereum.beacon.consensus;

import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.spec.BLSFunctions;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.function.Function;

/** Default implementation of {@link BeaconChainSpec}. */
public class BeaconChainSpecImpl implements BeaconChainSpec {
  private final SpecConstants constants;
  private final Function<BytesValue, Hash32> hashFunction;
  private final ObjectHasher<Hash32> objectHasher;
  private final BLSFunctions blsFunctions;
  private final boolean blsVerify;
  private final boolean blsVerifyProofOfPossession;
  private final boolean verifyDepositProof;
  private final boolean computableGenesisTime;

  public BeaconChainSpecImpl(
      SpecConstants constants,
      Function<BytesValue, Hash32> hashFunction,
      ObjectHasher<Hash32> objectHasher,
      BLSFunctions blsFunctions,
      boolean blsVerify,
      boolean blsVerifyProofOfPossession,
      boolean verifyDepositProof,
      boolean computableGenesisTime) {
    this.constants = constants;
    this.hashFunction = hashFunction;
    this.objectHasher = objectHasher;
    this.blsFunctions = blsFunctions;
    this.blsVerify = blsVerify;
    this.blsVerifyProofOfPossession = blsVerifyProofOfPossession;
    this.verifyDepositProof = verifyDepositProof;
    this.computableGenesisTime = computableGenesisTime;
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

  @Override
  public boolean isComputableGenesisTime() {
    return computableGenesisTime;
  }

  @Override
  public boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    return blsFunctions.bls_verify(publicKey, message, signature, domain);
  }

  @Override
  public boolean bls_verify_multiple(List<BLS381.PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
    return blsFunctions.bls_verify_multiple(publicKeys, messages, signature, domain);
  }

  @Override
  public BLS381.PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    return blsFunctions.bls_aggregate_pubkeys(publicKeysBytes);
  }
}
