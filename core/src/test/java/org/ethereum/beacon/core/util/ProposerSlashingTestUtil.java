package org.ethereum.beacon.core.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.spec.ChainSpec;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class ProposerSlashingTestUtil {
  private ProposerSlashingTestUtil() {}

  public static List<ProposerSlashing> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandom(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static ProposerSlashing createRandom(Random random) {
    ProposalSignedData signedData1 =
        new ProposalSignedData(
            UInt64.ZERO, ChainSpec.BEACON_CHAIN_SHARD_NUMBER, Hash32.random(random));
    ProposalSignedData signedData2 =
        new ProposalSignedData(
            UInt64.ZERO, ChainSpec.BEACON_CHAIN_SHARD_NUMBER, Hash32.random(random));
    return new ProposerSlashing(
        UInt24.ZERO, signedData1, Bytes96.random(random), signedData2, Bytes96.random(random));
  }
}
