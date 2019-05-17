package org.ethereum.beacon.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class DepositTestUtil {
  private DepositTestUtil() {}

  public static List<Deposit> createRandomList(
      Random random, SpecConstants spec, UInt64 startIndex, int maxCount) {
    List<Deposit> deposits = new ArrayList<>();
    int count = Math.abs(random.nextInt()) % maxCount + 1;
    for (int i = 0; i < count; i++) {
      deposits.add(createRandom(random, spec, startIndex.plus(i)));
    }
    return deposits;
  }

  public static Deposit createRandom(Random random, SpecConstants spec, UInt64 depositIndex) {
    DepositData depositData =
        new DepositData(
            BLSPubkey.wrap(Bytes48.random(random)),
            Hash32.random(random),
            spec.getMaxEffectiveBalance(), BLSSignature.wrap(Bytes96.random(random)));

    ReadVector<Integer, Hash32> merkleBranch =
        ReadVector.wrap(Collections.nCopies(spec.getDepositContractTreeDepth().getIntValue(), Hash32.ZERO), Integer::new);

    return new Deposit(merkleBranch, depositIndex, depositData);
  }
}
