package org.ethereum.beacon.core.util;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;

public abstract class AttestationTestUtil {
  private AttestationTestUtil() {}

  public static List<Attestation> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandomAttestation(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static Attestation createRandomAttestation(Random random) {
    return new Attestation(
        createRandomAttestationData(random),
        Bitfield.of(BytesValue.wrap(new byte[64])),
        Bitfield.of(BytesValue.wrap(new byte[64])),
        BLSSignature.wrap(Bytes96.random(random)));
  }

  public static AttestationData createRandomAttestationData(Random random) {
    return new AttestationData(
        ChainSpec.GENESIS_SLOT,
        ChainSpec.BEACON_CHAIN_SHARD_NUMBER,
        Hash32.random(random),
        Hash32.random(random),
        Hash32.random(random),
        Hash32.random(random),
        ChainSpec.DEFAULT.getGenesisEpoch(),
        Hash32.random(random));
  }
}
