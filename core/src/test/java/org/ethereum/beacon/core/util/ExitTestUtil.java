package org.ethereum.beacon.core.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.bytes.Bytes96;

public abstract class ExitTestUtil {
  private ExitTestUtil() {}

  public static List<Exit> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandom(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static Exit createRandom(Random random) {
    return new Exit(
        SlotNumber.ZERO, ValidatorIndex.ZERO, BLSSignature.wrap(Bytes96.random(random)));
  }
}
