package org.ethereum.beacon.simulator.util;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.MessageParameters;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SimulateUtils {

  private static Pair<List<Deposit>, List<BLS381.KeyPair>> cachedDeposits =
      Pair.with(new ArrayList<>(), new ArrayList<>());

  public static synchronized Pair<List<Deposit>, List<BLS381.KeyPair>> getAnyDeposits(
      Random rnd, SpecHelpers specHelpers, int count, boolean isProofVerifyEnabled) {
    if (count > cachedDeposits.getValue0().size()) {
      Pair<List<Deposit>, List<BLS381.KeyPair>> newDeposits =
          generateRandomDeposits(rnd, specHelpers, count - cachedDeposits.getValue0().size(),
              isProofVerifyEnabled);
      cachedDeposits.getValue0().addAll(newDeposits.getValue0());
      cachedDeposits.getValue1().addAll(newDeposits.getValue1());
    }
    return Pair.with(
        cachedDeposits.getValue0().subList(0, count), cachedDeposits.getValue1().subList(0, count));
  }

  public static synchronized Deposit getDepositForKeyPair(
      Random rnd, BLS381.KeyPair keyPair, SpecHelpers specHelpers, boolean isProofVerifyEnabled) {
    Hash32 proofOfPosession = Hash32.random(rnd);
    DepositInput depositInputWithoutSignature =
        new DepositInput(
            BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
            proofOfPosession,
            BLSSignature.wrap(Bytes96.ZERO));

    BLSSignature signature = BLSSignature.ZERO;

    if (isProofVerifyEnabled) {
      Hash32 msgHash = specHelpers.signed_root(depositInputWithoutSignature, "proofOfPossession");
      signature = BLSSignature.wrap(
          BLS381.sign(
              new MessageParameters.Impl(msgHash, SignatureDomains.DEPOSIT.toBytesBigEndian()),
              keyPair).getEncoded());
    }

    Deposit deposit =
        new Deposit(
            Collections.singletonList(Hash32.random(rnd)),
            UInt64.ZERO,
            new DepositData(
                specHelpers.getConstants().getMaxDepositAmount(),
                Time.of(0),
                new DepositInput(
                    BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
                    proofOfPosession,
                    signature)));
    return deposit;
  }

  public static synchronized List<Deposit> getDepositsForKeyPairs(
      Random rnd, List<BLS381.KeyPair> keyPairs, SpecHelpers specHelpers, boolean isProofVerifyEnabled) {
    List<Deposit> deposits = new ArrayList<>();

    for (BLS381.KeyPair keyPair : keyPairs) {
      deposits.add(getDepositForKeyPair(rnd, keyPair, specHelpers, isProofVerifyEnabled));
    }

    return deposits;
  }

  private static synchronized Pair<List<Deposit>, List<BLS381.KeyPair>> generateRandomDeposits(
      Random rnd, SpecHelpers specHelpers, int count, boolean isProofVerifyEnabled) {
    List<BLS381.KeyPair> validatorsKeys = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      BLS381.KeyPair keyPair = BLS381.KeyPair.create(PrivateKey.create(Bytes32.random(rnd)));
      validatorsKeys.add(keyPair);

    }
    List<Deposit> deposits = getDepositsForKeyPairs(rnd, validatorsKeys, specHelpers, isProofVerifyEnabled);
    return Pair.with(deposits, validatorsKeys);
  }
}
