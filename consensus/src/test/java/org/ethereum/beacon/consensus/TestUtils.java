package org.ethereum.beacon.consensus;

import static org.ethereum.beacon.core.spec.SignatureDomains.DEPOSIT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositMessage;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class TestUtils {

  private static Pair<List<Deposit>, List<KeyPair>> cachedDeposits =
      Pair.with(new ArrayList<>(), new ArrayList<>());

  public synchronized static Pair<List<Deposit>, List<KeyPair>> getAnyDeposits(Random rnd, BeaconChainSpec spec, int count) {
    if (count > cachedDeposits.getValue0().size()) {
      Pair<List<Deposit>, List<KeyPair>> newDeposits = generateRandomDeposits(rnd, spec,
          count - cachedDeposits.getValue0().size());
      cachedDeposits.getValue0().addAll(newDeposits.getValue0());
      cachedDeposits.getValue1().addAll(newDeposits.getValue1());
    }
    return Pair.with(cachedDeposits.getValue0().subList(0, count),
        cachedDeposits.getValue1().subList(0, count));
  }

  private synchronized static Pair<List<Deposit>, List<KeyPair>> generateRandomDeposits(Random rnd, BeaconChainSpec spec, int count) {
    List<Deposit> deposits = new ArrayList<>();
    List<KeyPair> validatorsKeys = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      KeyPair keyPair = KeyPair.create(PrivateKey.create(Bytes32.random(rnd)));
      Hash32 withdrawalCredentials = Hash32.random(rnd);
      DepositData depositDataWithoutSignature = new DepositData(
          BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
          withdrawalCredentials,
          spec.getConstants().getMaxEffectiveBalance(),
          BLSSignature.wrap(Bytes96.ZERO)
      );
      DepositMessage depositMessage = DepositMessage.from(depositDataWithoutSignature);
      Hash32 msgHash = spec.hash_tree_root(depositMessage);
      UInt64 domain = spec.compute_domain(DEPOSIT, Bytes4.ZERO);
      Signature signature = BLS381
          .sign(MessageParameters.create(msgHash, domain), keyPair);

      validatorsKeys.add(keyPair);

      Deposit deposit =
          Deposit.create(
              Collections.nCopies(
                  spec.getConstants().getDepositContractTreeDepthPlusOne().getIntValue(),
                  Hash32.random(rnd)),
              new DepositData(
                  BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
                  withdrawalCredentials,
                  spec.getConstants().getMaxEffectiveBalance(),
                  BLSSignature.wrap(signature.getEncoded())));
      deposits.add(deposit);
    }

    return Pair.with(deposits, validatorsKeys);
  }

  public static List<Deposit> generateRandomDepositsWithoutSig(Random rnd, BeaconChainSpec spec, int count) {
    List<Deposit> deposits = new ArrayList<>();

    UInt64 counter = UInt64.ZERO;
    for (int i = 0; i < count; i++) {
      Hash32 withdrawalCredentials = Hash32.random(rnd);

      BLSPubkey pubkey = BLSPubkey.wrap(Bytes48.leftPad(counter.toBytesBigEndian()));
      Deposit deposit =
          Deposit.create(
              Collections.singletonList(Hash32.random(rnd)),
              new DepositData(
                  pubkey,
                  withdrawalCredentials,
                  spec.getConstants().getMaxEffectiveBalance(),
                  BLSSignature.ZERO));
      deposits.add(deposit);
      counter = counter.increment();
    }
    return deposits;
  }
}
