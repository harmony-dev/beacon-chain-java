package org.ethereum.beacon.ssz.visitor;

import org.ethereum.beacon.crypto.Hashes;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SSZHasherMerkleizerTest {

  @Test
  public void testNextBinaryLog() {
    SSZSimpleHasher hasher = new SSZSimpleHasher(null, Hashes::sha256, 32);
    assertEquals(0, hasher.nextBinaryLog(0));
    assertEquals(0, hasher.nextBinaryLog(1));
    assertEquals(3, hasher.nextBinaryLog(7));
    assertEquals(3, hasher.nextBinaryLog(8));
    assertEquals(4, hasher.nextBinaryLog(9));
  }

  @Test
  public void testMerkleizeEqual() {
    SSZSimpleHasher hasher = new SSZSimpleHasher(null, Hashes::sha256, 32);

    List<BytesValue> input = new ArrayList<>();
    input.add(Hashes.sha256(BytesValue.fromHexString("aa")));
    input.add(Hashes.sha256(BytesValue.fromHexString("bb")));
    input.add(Hashes.sha256(BytesValue.fromHexString("cc")));
    MerkleTrie base1 = hasher.merkleize(input, 8) ;
    MerkleTrie virtual1 = hasher.merkleize(input, 4, 8);
    MerkleTrie base2 = hasher.merkleize(input, 16) ;
    MerkleTrie virtual2 = hasher.merkleize(input, 4, 16);
    MerkleTrie base3 = hasher.merkleize(input, 32) ;
    MerkleTrie virtual3 = hasher.merkleize(input, 4, 32);
    assertEquals(base1.getPureRoot(), virtual1.getPureRoot());
    assertEquals(base2.getPureRoot(), virtual2.getPureRoot());
    assertEquals(base3.getPureRoot(), virtual3.getPureRoot());
  }

  @Test
  public void testMerkleizeEmptyEqual() {
    SSZSimpleHasher hasher = new SSZSimpleHasher(null, Hashes::sha256, 32);

    List<BytesValue> input = new ArrayList<>();
    MerkleTrie base1 = hasher.merkleize(input, 8) ;
    MerkleTrie virtual1 = hasher.merkleize(input, 1, 8);
    MerkleTrie base2 = hasher.merkleize(input, 16) ;
    MerkleTrie virtual2 = hasher.merkleize(input, 1, 16);
    MerkleTrie base3 = hasher.merkleize(input, 32) ;
    MerkleTrie virtual3 = hasher.merkleize(input, 1, 32);
    assertEquals(base1.getPureRoot(), virtual1.getPureRoot());
    assertEquals(base2.getPureRoot(), virtual2.getPureRoot());
    assertEquals(base3.getPureRoot(), virtual3.getPureRoot());
  }
}
