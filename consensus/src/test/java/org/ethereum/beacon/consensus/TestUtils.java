package org.ethereum.beacon.consensus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters.Impl;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class TestUtils {

  private static Pair<List<Deposit>, List<KeyPair>> cachedDeposits =
      Pair.with(new ArrayList<>(), new ArrayList<>());

  public synchronized static Pair<List<Deposit>, List<KeyPair>> getAnyDeposits(Random rnd, SpecHelpers specHelpers, int count) {
    if (count > cachedDeposits.getValue0().size()) {
      Pair<List<Deposit>, List<KeyPair>> newDeposits = generateRandomDeposits(rnd, specHelpers,
          count - cachedDeposits.getValue0().size());
      cachedDeposits.getValue0().addAll(newDeposits.getValue0());
      cachedDeposits.getValue1().addAll(newDeposits.getValue1());
    }
    return Pair.with(cachedDeposits.getValue0().subList(0, count),
        cachedDeposits.getValue1().subList(0, count));
  }

  private synchronized static Pair<List<Deposit>, List<KeyPair>> generateRandomDeposits(Random rnd, SpecHelpers specHelpers, int count) {
    List<Deposit> deposits = new ArrayList<>();
    List<KeyPair> validatorsKeys = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      KeyPair keyPair = KeyPair.create(PrivateKey.create(Bytes32.random(rnd)));
      Hash32 proofOfPosession = Hash32.random(rnd);
      DepositInput depositInputWithoutSignature = new DepositInput(
          BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
          proofOfPosession,
          BLSSignature.wrap(Bytes96.ZERO)
      );
      Hash32 msgHash = specHelpers.hash_tree_root(depositInputWithoutSignature);
      Signature signature = BLS381
          .sign(new Impl(msgHash, SignatureDomains.DEPOSIT.toBytesBigEndian()), keyPair);

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
                      BLSSignature.wrap(signature.getEncoded()))
                  ));
      deposits.add(deposit);
    }

    return Pair.with(deposits, validatorsKeys);
  }

  public static List<Deposit> generateRandomDepositsWithoutSig(Random rnd, SpecHelpers specHelpers, int count) {
    List<Deposit> deposits = new ArrayList<>();

    UInt64 counter = UInt64.ZERO;
    for (int i = 0; i < count; i++) {
      Hash32 proofOfPosession = Hash32.random(rnd);

      BLSPubkey pubkey = BLSPubkey.wrap(Bytes48.leftPad(counter.toBytesBigEndian()));
      Deposit deposit =
          new Deposit(
              Collections.singletonList(Hash32.random(rnd)),
              counter,
              new DepositData(
                  specHelpers.getChainSpec().getMaxDepositAmount(),
                  Time.of(0),
                  new DepositInput(pubkey, proofOfPosession, BLSSignature.ZERO)));
      deposits.add(deposit);
      counter = counter.increment();
    }
    return deposits;
  }
}
