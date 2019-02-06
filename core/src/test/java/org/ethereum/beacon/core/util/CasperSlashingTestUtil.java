package org.ethereum.beacon.core.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;

public abstract class CasperSlashingTestUtil {
  private CasperSlashingTestUtil() {}

  public static List<CasperSlashing> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandom(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static CasperSlashing createRandom(Random random) {
    SlashableVoteData voteData1 =
        new SlashableVoteData(
            new UInt24[0],
            new UInt24[0],
            AttestationTestUtil.createRandomAttestationData(random),
            Bytes96.random(random));
    SlashableVoteData voteData2 =
        new SlashableVoteData(
            new UInt24[0],
            new UInt24[0],
            AttestationTestUtil.createRandomAttestationData(random),
            Bytes96.random(random));
    return new CasperSlashing(voteData1, voteData2);
  }
}
