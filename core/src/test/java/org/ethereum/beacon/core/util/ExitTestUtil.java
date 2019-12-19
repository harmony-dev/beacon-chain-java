package org.ethereum.beacon.core.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.bytes.Bytes96;

public abstract class ExitTestUtil {
  private ExitTestUtil() {}

  public static List<VoluntaryExit> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandom(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static VoluntaryExit createRandom(Random random) {
    return new VoluntaryExit(EpochNumber.ZERO, ValidatorIndex.ZERO);
  }
}
