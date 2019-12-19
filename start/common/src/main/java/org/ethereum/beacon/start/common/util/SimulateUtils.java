package org.ethereum.beacon.start.common.util;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositMessage;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.*;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SimulateUtils {

  private static Pair<List<Deposit>, List<BLS381.KeyPair>> cachedDeposits =
      Pair.with(new ArrayList<>(), new ArrayList<>());

  public static synchronized Pair<List<Deposit>, List<BLS381.KeyPair>> getAnyDeposits(
      Random rnd, BeaconChainSpec spec, int count, boolean isProofVerifyEnabled) {
    if (count > cachedDeposits.getValue0().size()) {
      Pair<List<Deposit>, List<BLS381.KeyPair>> newDeposits =
          generateRandomDeposits(UInt64.valueOf(cachedDeposits.getValue0().size()), rnd, spec,
              count - cachedDeposits.getValue0().size(), isProofVerifyEnabled);
      cachedDeposits.getValue0().addAll(newDeposits.getValue0());
      cachedDeposits.getValue1().addAll(newDeposits.getValue1());
    }
    return Pair.with(
        cachedDeposits.getValue0().subList(0, count), cachedDeposits.getValue1().subList(0, count));
  }

  public static Deposit getDepositForKeyPair(Random rnd,
      BLS381.KeyPair keyPair, BeaconChainSpec spec, boolean isProofVerifyEnabled) {
    return getDepositForKeyPair(
        rnd,
        keyPair,
        spec,
        spec.getConstants().getMaxEffectiveBalance(),
        isProofVerifyEnabled);
  }

  public static synchronized Deposit getDepositForKeyPair(Random rnd,
      BLS381.KeyPair keyPair, BeaconChainSpec spec, Gwei initBalance, boolean isProofVerifyEnabled) {
    Hash32 credentials = Hash32.random(rnd);
    List<Hash32> depositProof = Collections.singletonList(Hash32.random(rnd));
    return getDepositForKeyPair(
        keyPair, credentials, initBalance, depositProof, spec, isProofVerifyEnabled);
  }

  @NotNull
  private static Deposit getDepositForKeyPair(
      BLS381.KeyPair keyPair,
      Hash32 withdrawalCredentials,
      Gwei initBalance,
      List<Hash32> depositProof,
      BeaconChainSpec spec,
      boolean isProofVerifyEnabled) {
    DepositData depositDataWithoutSignature =
        new DepositData(
            BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
            withdrawalCredentials,
            initBalance,
            BLSSignature.wrap(Bytes96.ZERO));
    DepositMessage depositMessage = DepositMessage.from(depositDataWithoutSignature);

    BLSSignature signature = BLSSignature.ZERO;

    if (isProofVerifyEnabled) {
      Hash32 msgHash = spec.hash_tree_root(depositMessage);
      UInt64 domain = spec.compute_domain(SignatureDomains.DEPOSIT, Bytes4.ZERO);
      signature =
          BLSSignature.wrap(
              BLS381.sign(MessageParameters.create(msgHash, domain), keyPair).getEncoded());
    }

    Deposit deposit =
        Deposit.create(
            depositProof,
            new DepositData(
                depositDataWithoutSignature.getPubKey(),
                depositDataWithoutSignature.getWithdrawalCredentials(),
                depositDataWithoutSignature.getAmount(),
                signature));
    return deposit;
  }

  public static synchronized List<Deposit> getDepositsForKeyPairs(
      UInt64 startIndex,
      Random rnd,
      List<BLS381.KeyPair> keyPairs,
      BeaconChainSpec spec,
      boolean isProofVerifyEnabled) {
    return getDepositsForKeyPairs(
        startIndex,
        rnd,
        keyPairs,
        spec,
        spec.getConstants().getMaxEffectiveBalance(),
        isProofVerifyEnabled);
  }

  public static synchronized List<Deposit> getDepositsForKeyPairs(
      UInt64 startIndex,
      Random rnd,
      List<BLS381.KeyPair> keyPairs,
      BeaconChainSpec spec,
      Gwei initBalance,
      boolean isProofVerifyEnabled) {
    List<Hash32> withdrawalCredentials = generateRandomWithdrawalCredentials(rnd, keyPairs);
    List<List<Hash32>> depositProofs = generateRandomDepositProofs(rnd, keyPairs);
    return getDepositsForKeyPairs(
        keyPairs, withdrawalCredentials, initBalance, depositProofs, spec, isProofVerifyEnabled);
  }

  @NotNull
  public static List<Deposit> getDepositsForKeyPairs(
      List<BLS381.KeyPair> keyPairs,
      List<Hash32> withdrawalCredentials,
      Gwei initBalance,
      List<List<Hash32>> depositProofs,
      BeaconChainSpec spec,
      boolean isProofVerifyEnabled) {
    List<Deposit> deposits = new ArrayList<>();

    for (int i = 0; i < keyPairs.size(); i++) {
      BLS381.KeyPair keyPair = keyPairs.get(i);
      Hash32 credentials = withdrawalCredentials.get(i);
      List<Hash32> depositProof = depositProofs.get(i);
      deposits.add(getDepositForKeyPair(
          keyPair, credentials, initBalance, depositProof, spec, isProofVerifyEnabled));
    }

    return deposits;
  }

  @NotNull
  public static List<List<Hash32>> generateRandomDepositProofs(
      Random rnd, List<BLS381.KeyPair> keyPairs) {
    List<List<Hash32>> depositProofs = new ArrayList<>();
    for (BLS381.KeyPair keyPair : keyPairs) {
      List<Hash32> depositProof = Collections.singletonList(Hash32.random(rnd));
      depositProofs.add(depositProof);
    }
    return depositProofs;
  }

  @NotNull
  public static List<Hash32> generateRandomWithdrawalCredentials(
      Random rnd, List<BLS381.KeyPair> keyPairs) {
    List<Hash32> withdrawalCredentials = new ArrayList<>();
    for (BLS381.KeyPair keyPair : keyPairs) {
      withdrawalCredentials.add(Hash32.random(rnd));
    }
    return withdrawalCredentials;
  }

  /**
   * Generate withdrawal credentials according to mocked start spec.
   * @See <a href="https://github.com/ethereum/eth2.0-pm/tree/master/interop/mocked_start#generate-deposits"/>
   */
  public static List<Hash32> generateInteropCredentials(
      UInt64 blsWithdrawalPrefix, List<BLS381.KeyPair> keyPairs) {
    List<Hash32> withdrawalCredentials = new ArrayList<>();
    for (BLS381.KeyPair keyPair : keyPairs) {
      MutableBytes32 credentials =
          Hashes.sha256(keyPair.getPublic().getEncodedBytes()).mutableCopy();
      credentials.set(0, blsWithdrawalPrefix.toBytes8().get(0));
      withdrawalCredentials.add(Hash32.wrap(credentials.copy()));
    }
    return withdrawalCredentials;
  }

  private static synchronized Pair<List<Deposit>, List<BLS381.KeyPair>> generateRandomDeposits(
      UInt64 startIndex, Random rnd, BeaconChainSpec spec, int count, boolean isProofVerifyEnabled) {
    List<BLS381.KeyPair> validatorsKeys = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      BLS381.KeyPair keyPair = BLS381.KeyPair.create(PrivateKey.create(Bytes32.random(rnd)));
      validatorsKeys.add(keyPair);

    }
    List<Deposit> deposits = getDepositsForKeyPairs(startIndex, rnd, validatorsKeys, spec, isProofVerifyEnabled);
    return Pair.with(deposits, validatorsKeys);
  }
}
