package org.ethereum.beacon.core.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;

public abstract class ProposerSlashingTestUtil {
  private ProposerSlashingTestUtil() {}

  public static List<ProposerSlashing> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandom(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static ProposerSlashing createRandom(Random random) {
    BeaconBlockHeader header1 =
        new BeaconBlockHeader(
            SpecConstants.GENESIS_SLOT, Hash32.random(random), Hash32.random(random), Hash32.random(random),
            BLSSignature.wrap(Bytes96.random(random)));
    BeaconBlockHeader header2 =
        new BeaconBlockHeader(
            SpecConstants.GENESIS_SLOT, Hash32.random(random), Hash32.random(random), Hash32.random(random),
            BLSSignature.wrap(Bytes96.random(random)));
    return new ProposerSlashing(
        ValidatorIndex.ZERO,
        header1,
        header2);
  }
}
