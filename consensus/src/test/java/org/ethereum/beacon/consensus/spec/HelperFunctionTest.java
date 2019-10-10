package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.Time;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class HelperFunctionTest {

  private static BeaconChainSpec createSpec() {
    SpecConstants constants = BeaconChainSpec.DEFAULT_CONSTANTS;
    return new BeaconChainSpec.Builder()
        .withConstants(constants)
        .withDefaultHashFunction()
        .withDefaultHasher(constants)
        .withBlsVerify(false)
        .build();
  }

  @Test(expected = SpecCommons.SpecAssertionFailed.class)
  public void testGetIndexedAttestation_invalid_custody_bits() {
    BeaconChainSpec spec = createSpec();

    List<Deposit> deposits =
        TestUtils.generateRandomDepositsWithoutSig(
            new Random(), spec, spec.getConstants().getSlotsPerEpoch().intValue() * 4);
    Date genesisDate = new Date();

    BeaconState genesis =
        spec.initialize_beacon_state_from_eth1(
            spec.hash_tree_root(123), Time.of(genesisDate.getTime() / 1000), deposits);

    // create an attestation, where custody bits are not subset of aggregation bits
    Bitlist aggregationBits = Bitlist.of(4, Arrays.asList(0, 1, 2), -1);
    Bitlist custodyBits = Bitlist.of(4, Arrays.asList(3), -1);

    Attestation attestation =
        new Attestation(
            aggregationBits,
            new AttestationData(
                Hash32.ZERO,
                new Checkpoint(new EpochNumber(0), Hash32.ZERO),
                new Checkpoint(new EpochNumber(0), Hash32.ZERO),
                new Crosslink(
                    new ShardNumber(0),
                    Hash32.ZERO,
                    new EpochNumber(0),
                    new EpochNumber(0),
                    Hash32.ZERO)),
            custodyBits,
            BLSSignature.ZERO,
            spec.getConstants());

    spec.get_indexed_attestation(genesis, attestation);
  }
}
