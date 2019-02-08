package org.ethereum.beacon.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.core.types.BLSSignature;
import tech.pegasys.artemis.util.bytes.Bytes96;

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
            Collections.emptyList(),
            Collections.emptyList(),
            AttestationTestUtil.createRandomAttestationData(random),
            BLSSignature.wrap(Bytes96.random(random)));

    SlashableVoteData voteData2 =
        new SlashableVoteData(
            Collections.emptyList(),
            Collections.emptyList(),
            AttestationTestUtil.createRandomAttestationData(random),
            BLSSignature.wrap(Bytes96.random(random)));

    return new CasperSlashing(voteData1, voteData2);
  }
}
