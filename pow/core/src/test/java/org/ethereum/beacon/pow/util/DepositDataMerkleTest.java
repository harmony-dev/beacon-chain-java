package org.ethereum.beacon.pow.util;

import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.pow.DepositIncrementalMerkle;
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
import static org.junit.Assert.fail;

public class DepositDataMerkleTest {
  @Test
  public void test() {
    MerkleTree<DepositData> simple = new DepositSimpleMerkle(Hashes::sha256, 32);
    MerkleTree<DepositData> incremental = new DepositIncrementalMerkle(Hashes::sha256, 32, 3);
    Consumer<Integer> insertInBoth =
        integer -> {
          simple.insertValue(createDepositData(integer));
          incremental.insertValue(createDepositData(integer));
        };
    for (int i = 1; i < 20; ++i) {
      insertInBoth.accept(i);
      assertEquals(
          simple.getDepositRoot(UInt64.valueOf(i - 1)),
          incremental.getDepositRoot(UInt64.valueOf(i - 1)));
    }
    int someIndex = simple.getLastIndex() + 1;
    for (int i = 20; i < 22; ++i) {
      insertInBoth.accept(i);
      assertArrayEquals(
          simple.getProof(someIndex, i).listCopy().toArray(),
          incremental.getProof(someIndex, i).listCopy().toArray());
    }
    insertInBoth.accept(22);
    try {
      simple.getProof(someIndex,22);
      fail();
    } catch (RuntimeException e) {

    }
    try {
      incremental.getProof(someIndex,22);
      fail();
    } catch (RuntimeException e) {

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