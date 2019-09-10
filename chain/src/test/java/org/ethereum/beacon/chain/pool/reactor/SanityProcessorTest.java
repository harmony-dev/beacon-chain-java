package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.PoolTestConfigurator;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SanityChecker;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.time.Duration;

class SanityProcessorTest extends PoolTestConfigurator {

    private SanityProcessor sanityProcessor;

    @BeforeEach
    void setUp() {
        final SanityChecker sanityChecker = new SanityChecker(spec);
        sanityProcessor = new SanityProcessor(sanityChecker, schedulers, source, finalizedCheckpoints);
        final Checkpoint checkpoint = Checkpoint.EMPTY;
        finalizedCheckpoints.onNext(checkpoint);

        schedulers.addTime(Duration.ofSeconds(5));
    }

    @Test
    void testValidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        source.onNext(attestation);
//TODO: assertThat
//        StepVerifier.create(sanityProcessor.getValid())
//                .expectNext(attestation)
//                .verifyComplete();
    }

    @Test
    void testInvalidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        source.onNext(attestation);
//TODO: assertThat
//        StepVerifier.create(sanityProcessor.getInvalid())
//                .expectNext(attestation)
//                .verifyComplete();
    }
}
