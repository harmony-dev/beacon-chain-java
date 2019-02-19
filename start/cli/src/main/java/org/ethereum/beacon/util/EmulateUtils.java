package org.ethereum.beacon.util;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.MessageParameters;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EmulateUtils {
  private static final Random rnd = new Random();

  private static Pair<List<Deposit>, List<BLS381.KeyPair>> cachedDeposits =
      Pair.with(new ArrayList<>(), new ArrayList<>());

  public static synchronized Pair<List<Deposit>, List<BLS381.KeyPair>> getAnyDeposits(
      SpecHelpers specHelpers, int count) {
    if (count > cachedDeposits.getValue0().size()) {
      Pair<List<Deposit>, List<BLS381.KeyPair>> newDeposits =
          generateRandomDeposits(specHelpers, count - cachedDeposits.getValue0().size());
      cachedDeposits.getValue0().addAll(newDeposits.getValue0());
      cachedDeposits.getValue1().addAll(newDeposits.getValue1());
    }
    return Pair.with(
        cachedDeposits.getValue0().subList(0, count), cachedDeposits.getValue1().subList(0, count));
  }

  private static synchronized Pair<List<Deposit>, List<BLS381.KeyPair>> generateRandomDeposits(
      SpecHelpers specHelpers, int count) {
    List<Deposit> deposits = new ArrayList<>();
    List<BLS381.KeyPair> validatorsKeys = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      BLS381.KeyPair keyPair = BLS381.KeyPair.generate();
      Hash32 proofOfPosession = Hash32.random(rnd);
      DepositInput depositInputWithoutSignature =
          new DepositInput(
              BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
              proofOfPosession,
              BLSSignature.wrap(Bytes96.ZERO));
      Hash32 msgHash = specHelpers.hash_tree_root(depositInputWithoutSignature);
      BLS381.Signature signature =
          BLS381.sign(
              new MessageParameters.Impl(msgHash, SignatureDomains.DEPOSIT.toBytesBigEndian()),
              keyPair);

      validatorsKeys.add(keyPair);

      Deposit deposit =
          new Deposit(
              Collections.singletonList(Hash32.random(rnd)),
              UInt64.ZERO,
              new DepositData(
                  specHelpers.getChainSpec().getMaxDepositAmount(),
                  Time.of(0),
                  new DepositInput(
                      BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
                      proofOfPosession,
                      BLSSignature.wrap(signature.getEncoded()))));
      deposits.add(deposit);
    }

    return Pair.with(deposits, validatorsKeys);
  }
}