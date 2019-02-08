package org.ethereum.beacon.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableAttestation;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import tech.pegasys.artemis.util.bytes.Bytes96;

public abstract class AttesterSlashingTestUtil {
  private AttesterSlashingTestUtil() {}

  public static List<AttesterSlashing> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandom(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static AttesterSlashing createRandom(Random random) {
    return new AttesterSlashing(
        new SlashableAttestation(
            Collections.emptyList(),
            AttestationTestUtil.createRandomAttestationData(random),
            Bitfield.EMPTY,
            BLSSignature.wrap(Bytes96.random(random))),
        new SlashableAttestation(
            Collections.emptyList(),
            AttestationTestUtil.createRandomAttestationData(random),
            Bitfield.EMPTY,
            BLSSignature.wrap(Bytes96.random(random)))
    );
  }
}
