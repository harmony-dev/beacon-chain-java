package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.PoolTestConfigurator;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.DirectProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ethereum.beacon.chain.pool.AttestationPool.MAX_ATTESTATION_LOOKAHEAD;
import static org.ethereum.beacon.chain.pool.AttestationPool.MAX_PROCESSED_ATTESTATIONS;

class MultiProcessorTest extends PoolTestConfigurator {

    private final Checkpoint checkpoint = Checkpoint.EMPTY;
    private final SlotNumber slotNumber = SlotNumber.of(100L);
    private DirectProcessor<SlotNumber> newSlots = DirectProcessor.create();

    private TimeProcessor timeProcessor;
    private SanityProcessor sanityProcessor;
    private DoubleWorkProcessor doubleWorkProcessor;

    @BeforeEach
    void setUp() {
        final TimeFrameFilter timeFrameFilter = new TimeFrameFilter(spec, MAX_ATTESTATION_LOOKAHEAD);
        timeProcessor = new TimeProcessor(timeFrameFilter, schedulers, source, finalizedCheckpoints, newSlots);

        final SanityChecker sanityChecker = new SanityChecker(spec);
        sanityProcessor = new SanityProcessor(sanityChecker, schedulers, source, finalizedCheckpoints);

        final ProcessedAttestations processedAttestations = new ProcessedAttestations(spec::hash_tree_root, MAX_PROCESSED_ATTESTATIONS);
        doubleWorkProcessor = new DoubleWorkProcessor(processedAttestations, schedulers, source);

        finalizedCheckpoints.onNext(this.checkpoint);
        newSlots.onNext(slotNumber);

        schedulers.addTime(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Process attestation by multiple processors")
    void processAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        source.onNext(attestation);

        schedulers.addTime(Duration.ofSeconds(5));

        timeProcessor.subscribe(s -> {
            assertThat(s.getSender())
                    .isNotNull()
                    .isEqualTo(sender);

            assertThat(s.getMessage())
                    .isNotNull()
                    .isEqualTo(message);
        });

        //TODO: add assert to sanity checker

        doubleWorkProcessor.subscribe(s -> {
            assertThat(s.getSender())
                    .isNotNull()
                    .isEqualTo(attestation.getSender());

            assertThat(s.getMessage())
                    .isNotNull()
                    .isEqualTo(attestation.getMessage());
        });
    }
}
