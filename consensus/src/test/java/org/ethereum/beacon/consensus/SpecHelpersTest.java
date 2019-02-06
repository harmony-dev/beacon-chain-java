package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.Hasher;
import org.ethereum.beacon.ssz.SSZHasher;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes3;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SpecHelpersTest {

  @Test
  public void shuffleTest0() throws Exception {
    SpecHelpers specHelpers = new SpecHelpers(null);

    int[] sample = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    BytesValue bytes = BytesValue.wrap(new byte[]{(byte) 1, (byte) 256, (byte) 65656});

    int expectedInt = 817593;
    Hash32 hash = Hashes.keccak256(bytes);
    int res = Bytes3.wrap(hash, 0).asUInt24BigEndian().getValue();


//    int[] actual = specHelpers.shuffle(sample, Hashes.keccak256(bytes));
//    int[] expected = new int[]{2, 4, 10, 7, 5, 6, 9, 8, 1, 3};
//
//    Assert.assertArrayEquals(expected, actual);
  }
  @Test
  public void shuffleTest1() throws Exception {
    int[] statuses = new int[]{
        2, 4, 0, 0, 2, 2, 4, 2, 3, 1, 0, 3, 3, 4, 4, 4, 1, 1, 1, 1,
        3, 2, 3, 0, 2, 4, 0, 2, 4, 0, 0, 4, 2, 1, 4, 1, 4, 2, 2, 1, 2, 4, 0, 4, 0, 3,
        0, 4, 4, 0, 0, 1, 3, 3, 0, 4, 3, 1, 1, 3, 1, 0, 0, 1, 0, 0, 4, 1, 2, 0, 1, 4,
        2, 1, 1, 4, 1, 1, 1, 1, 0, 4, 4, 0, 1, 3, 4, 2, 0, 1, 4, 3, 1, 2, 4, 2, 2, 2,
        3, 3, 3, 0, 2, 0, 4, 1, 1, 3, 0, 3, 1, 3, 4, 3, 3, 4, 0, 1, 0, 3, 3, 1, 4, 2,
        0, 3, 2, 3, 0, 4, 3, 1, 3, 3, 4, 3, 0, 0, 1, 0, 2, 4, 1, 3, 1, 3, 2, 4, 2, 2,
        0, 3, 2, 3, 1, 3, 0, 2, 1, 3, 2, 2, 1, 3, 0, 2, 1, 3, 2, 2, 2, 0, 0, 0, 3, 4,
        1, 4, 4, 3, 3, 0, 1, 2, 4, 1, 4, 0, 0, 4, 3, 2, 4, 3, 1, 2, 0, 4, 4, 2, 0, 4,
        4, 4, 4, 0, 1, 4, 4, 3, 0, 3, 2, 1, 4, 3, 0, 3, 0, 3, 1, 3, 3, 2, 3, 2, 2, 2,
        1, 0, 4, 2, 0, 4, 2, 2, 0, 1, 0, 0, 2, 0, 3, 3, 2, 4, 0, 3, 1, 0, 3, 4, 2, 4,
        0, 1, 4, 1, 0, 0, 4, 3, 3, 1, 1, 4, 1, 3, 1, 0, 4, 3, 3, 0, 2, 1, 3, 4, 1, 3,
        3, 3, 0, 4, 2, 3, 0, 0, 0, 1, 4, 3, 1, 4, 2, 0, 4, 2, 3, 0, 1, 2, 0, 4, 0, 4,
        4, 2, 1, 3, 4, 3, 2, 3, 3, 4, 3, 2, 2, 1, 3, 0, 3, 2, 1, 0, 1, 3, 2, 0, 0, 0,
        1, 1, 2, 2, 0, 3, 1, 0, 3, 2, 0, 0, 2, 3, 0, 0, 4, 4, 2, 0, 1, 1, 3, 0, 1, 0,
        1, 1, 3, 4, 0, 0, 3, 4, 4, 4, 0, 2, 4, 4, 1, 0, 2, 2, 3, 4, 4, 0, 1, 3, 2, 4,
        0, 1, 2, 1, 3, 3, 0, 3, 4, 1, 3, 1, 0, 1, 0, 4, 4, 3, 4, 1, 0, 3, 1, 3
    };
    // 148

    List<Integer> activeValidatorIndices = new ArrayList<>();
    for (int i = 0; i < statuses.length; i++) {
      if (statuses[i] == 1 || statuses[i] == 2) {
        activeValidatorIndices.add(i);
      }
    }

    SpecHelpers specHelpers = new SpecHelpers(null);

    Map<Integer, Long> map = Arrays.stream(statuses).boxed().collect
        (Collectors.groupingBy(Function.identity(), Collectors.counting()));


    System.out.println(map);
  }

  private DepositInput createDepositInput() {
    DepositInput depositInput =
        new DepositInput(
            BLSPubkey.wrap(Bytes48.TRUE),
            Hashes.keccak256(BytesValue.fromHexString("aa")),
            BLSSignature.wrap(Bytes96.ZERO));

    return depositInput;
  }

  @Test
  public void testHashTreeRoot1() {
    Function<Function<BytesValue, Hash32>, Hasher<Hash32>> objectHasherBuilder =
        bytesValueHash32Function ->
            SSZHasher.simpleHasher(
                bytesValue -> bytesValueHash32Function.apply(bytesValue).slice(0));
    SpecHelpers specHelpers = new SpecHelpers(null, objectHasherBuilder);
    Hash32 expected =
        Hash32.fromHexString("0x8fc89d0f1f435b07543b15fdf687e7fce4a754ecd9e5afbf8f0e83928a7f798f");
    Hash32 actual = specHelpers.hash_tree_root(createDepositInput());
    assertEquals(expected, actual);
  }
}
