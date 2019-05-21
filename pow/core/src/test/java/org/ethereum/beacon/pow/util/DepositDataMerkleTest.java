package org.ethereum.beacon.pow.util;

import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.pow.DepositBufferedMerkle;
import org.ethereum.beacon.pow.DepositSimpleMerkle;
import org.ethereum.beacon.pow.MerkleTree;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.function.Consumer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DepositDataMerkleTest {
  @Test
  public void test() {
    MerkleTree<DepositData> simple = new DepositSimpleMerkle(Hashes::sha256, 32);
    MerkleTree<DepositData> incremental = new DepositBufferedMerkle(Hashes::sha256, 32, 3);
    Consumer<Integer> insertInBoth =
        integer -> {
          simple.addValue(createDepositData(integer));
          incremental.addValue(createDepositData(integer));
        };
    for (int i = 1; i < 20; ++i) {
      System.out.println(i);
      insertInBoth.accept(i);
      assertEquals(
          simple.getRoot(i - 1),
          incremental.getRoot(i - 1));
    }
    int someIndex = simple.getLastIndex() + 1;
    for (int i = 20; i < 40; ++i) {
      insertInBoth.accept(i);
      assertArrayEquals(
          simple.getProof(someIndex, i).toArray(),
          incremental.getProof(someIndex, i).toArray());
    }
  }

  private DepositData createDepositData(int num) {
    return new DepositData(
        BLSPubkey.wrap(
            Bytes48.leftPad(
                BytesValue.wrap(UInt64.valueOf(num).toBytes8LittleEndian().extractArray()))),
        Hash32.ZERO,
        Gwei.ZERO,
        BLSSignature.ZERO);
  }
}
