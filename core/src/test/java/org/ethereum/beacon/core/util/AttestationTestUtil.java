package org.ethereum.beacon.core.util;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AttestationTestUtil {
  private AttestationTestUtil() {}

  public static List<Attestation> createRandomList(Random random, int maxCount) {
    return Stream.generate(() -> createRandomAttestation(random))
        .limit(Math.abs(random.nextInt()) % maxCount + 1)
        .collect(Collectors.toList());
  }

  public static Attestation createRandomAttestation(Random random) {
    return new Attestation(
        Bitlist.of(64),
        createRandomAttestationData(random),
        BLSSignature.wrap(Bytes96.random(random)),
        BeaconChainSpec.DEFAULT_CONSTANTS);
  }

  public static AttestationData createRandomAttestationData(Random random) {
    return new AttestationData(
        new SlotNumber(0),
        new CommitteeIndex(0),
        Hash32.random(random),
        new Checkpoint(BeaconChainSpec.DEFAULT_CONSTANTS.getGenesisEpoch(), Hash32.random(random)),
        new Checkpoint(
            BeaconChainSpec.DEFAULT_CONSTANTS.getGenesisEpoch().increment(), Hash32.random(random)));
  }
}
