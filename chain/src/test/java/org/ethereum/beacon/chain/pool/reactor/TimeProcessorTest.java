package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.test.StepVerifier;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.uint.UInt64;

import static org.ethereum.beacon.chain.pool.AttestationPool.MAX_ATTESTATION_LOOKAHEAD;

class TimeProcessorTest {

    private final SpecConstants specConstants =
            new SpecConstants() {
                @Override
                public SlotNumber getGenesisSlot() {
                    return SlotNumber.of(12345);
                }

                @Override
                public Time getSecondsPerSlot() {
                    return Time.of(1);
                }
            };

    private final BeaconChainSpec spec = BeaconChainSpec.Builder.createWithDefaultParams()
            .withConstants(new SpecConstants() {
                @Override
                public ShardNumber getShardCount() {
                    return ShardNumber.of(16);
                }

                @Override
                public SlotNumber.EpochLength getSlotsPerEpoch() {
                    return new SlotNumber.EpochLength(UInt64.valueOf(4));
                }
            })
            .withComputableGenesisTime(false)
            .withVerifyDepositProof(false)
            .withBlsVerifyProofOfPossession(false)
            .withBlsVerify(false)
            .withCache(true)
            .build();

    private final Checkpoint checkpoint = Checkpoint.EMPTY;
    private final SlotNumber slotNumber = SlotNumber.of(100L);
    private final ControlledSchedulers schedulers = Schedulers.createControlled();

    private TimeProcessor timeProcessor;
    private DirectProcessor<ReceivedAttestation> source = DirectProcessor.create();
    final FluxSink<ReceivedAttestation> sourceSink = source.sink();

    private Flux<Checkpoint> finalizedCheckpoints = Flux.empty();
    private Flux<SlotNumber> newSlots = Flux.empty();

    @BeforeEach
    void setUp() {

        final TimeFrameFilter timeFrameFilter = new TimeFrameFilter(spec, MAX_ATTESTATION_LOOKAHEAD);
        timeFrameFilter.feedFinalizedCheckpoint(checkpoint);
        timeFrameFilter.feedNewSlot(slotNumber);


        timeProcessor = new TimeProcessor(timeFrameFilter, schedulers, source, finalizedCheckpoints, newSlots);
        StepVerifier.create(timeProcessor)
                .verifyComplete();

//        timeProcessor.blockFirst()
//        sourceSink.next();

//        schedulers.addTime(1);

    }


    @Test
    void testValidAttestation() {

    }

    private Attestation createAttestation(BytesValue someValue) {
        final AttestationData attestationData = createAttestationData();

        return new Attestation(
                Bitlist.of(someValue.size() * 8, someValue, specConstants.getMaxValidatorsPerCommittee().getValue()),
                attestationData,
                Bitlist.of(8, BytesValue.fromHexString("bb"), specConstants.getMaxValidatorsPerCommittee().getValue()),
                BLSSignature.wrap(Bytes96.fromHexString("cc")),
                specConstants);
    }

    private AttestationData createAttestationData() {

        return new AttestationData(
                Hashes.sha256(BytesValue.fromHexString("aa")),
                new Checkpoint(EpochNumber.of(231), Hashes.sha256(BytesValue.fromHexString("bb"))),
                new Checkpoint(EpochNumber.of(1), Hashes.sha256(BytesValue.fromHexString("cc"))),
                Crosslink.EMPTY);
    }

}
