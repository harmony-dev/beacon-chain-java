package org.ethereum.beacon.core;

import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.crypto.Hashes;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes3;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpecHelpersTest {

  @Test
  public void shuffleTest0() throws Exception {
    SpecHelpers specHelpers = new SpecHelpers(null);

    int[] sample = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    BytesValue bytes = BytesValue.wrap(new byte[]{(byte) 1, (byte) 256, (byte) 65656});

    int expectedInt = 817593;
    Hash32 hash = Hashes.keccack256(bytes);
    int res = Bytes3.wrap(hash, 0).asUInt24BigEndian().getValue();


    int[] actual = specHelpers.shuffle(sample, Hashes.keccack256(bytes));
    int[] expected = new int[]{2, 4, 10, 7, 5, 6, 9, 8, 1, 3};

    Assert.assertArrayEquals(expected, actual);
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
    ShardCommittee[][] shuffling = specHelpers.get_shuffling(
        Hash32.fromHexString("0xc0c7f226fbd574a8c63dc26864c27833ea931e7c70b34409ba765f3d2031633d"),
        activeValidatorIndices.stream().mapToInt(i -> i).toArray(),
        210
    );

    Map<Integer, Long> map = Arrays.stream(statuses).boxed().collect
        (Collectors.groupingBy(Function.identity(), Collectors.counting()));


    System.out.println(map);
  }
}
