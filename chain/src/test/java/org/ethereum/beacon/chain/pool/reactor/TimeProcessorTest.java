package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.PoolTestConfigurator;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.TimeFrameFilter;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.DirectProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TimeProcessorTest extends PoolTestConfigurator {

    private final Checkpoint checkpoint = Checkpoint.EMPTY;
    private final SlotNumber slotNumber = SlotNumber.of(100L);
    private final DirectProcessor<SlotNumber> newSlots = DirectProcessor.create();

    private TimeProcessor timeProcessor;

    @BeforeEach
    void setUp() {

        final TimeFrameFilter timeFrameFilter = new TimeFrameFilter(spec, EpochNumber.of(233));
        timeProcessor = new TimeProcessor(timeFrameFilter, schedulers, source, finalizedCheckpoints, newSlots);
        finalizedCheckpoints.onNext(this.checkpoint);
        newSlots.onNext(slotNumber);

        schedulers.addTime(Duration.ofSeconds(5));
    }

    @Test
    void testValidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        source.onNext(attestation);

        timeProcessor.subscribe(s -> {
            assertThat(s.getSender())
                    .isNotNull()
                    .isEqualTo(sender);

            assertThat(s.getMessage())
                    .isNotNull()
                    .isEqualTo(message);
        });
    }
}
