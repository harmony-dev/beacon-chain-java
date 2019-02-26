package org.ethereum.beacon.core.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.Proposal;
import org.ethereum.beacon.core.spec.ChainSpec;
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
    Proposal signedData1 =
        new Proposal(
            ChainSpec.GENESIS_SLOT, ChainSpec.BEACON_CHAIN_SHARD_NUMBER, Hash32.random(random), BLSSignature.ZERO);
    Proposal signedData2 =
        new Proposal(
            ChainSpec.GENESIS_SLOT, ChainSpec.BEACON_CHAIN_SHARD_NUMBER, Hash32.random(random), BLSSignature.ZERO);
    return new ProposerSlashing(
        ValidatorIndex.ZERO,
        signedData1,
        BLSSignature.wrap(Bytes96.random(random)),
        signedData2,
        BLSSignature.wrap(Bytes96.random(random)));
  }
}
