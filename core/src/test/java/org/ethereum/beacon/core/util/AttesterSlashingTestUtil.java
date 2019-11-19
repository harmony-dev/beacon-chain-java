package org.ethereum.beacon.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import tech.pegasys.artemis.util.bytes.Bytes96;

public abstract class AttesterSlashingTestUtil {
  private AttesterSlashingTestUtil() {}

  public static List<AttesterSlashing> createRandomList(Random random, int maxCount, SpecConstants specConstants) {
    return Stream.generate(() -> createRandom(random, specConstants))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static AttesterSlashing createRandom(Random random, SpecConstants specConstants) {
    return new AttesterSlashing(
        new IndexedAttestation(
            Collections.emptyList(),
            AttestationTestUtil.createRandomAttestationData(random),
            BLSSignature.wrap(Bytes96.random(random)),
            specConstants),
        new IndexedAttestation(
            Collections.emptyList(),
            AttestationTestUtil.createRandomAttestationData(random),
            BLSSignature.wrap(Bytes96.random(random)),
            specConstants)
    );
  }
}
