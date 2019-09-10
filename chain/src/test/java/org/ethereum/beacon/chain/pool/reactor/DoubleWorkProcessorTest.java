package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.PoolTestConfigurator;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.ProcessedAttestations;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ethereum.beacon.chain.pool.AttestationPool.MAX_PROCESSED_ATTESTATIONS;

class DoubleWorkProcessorTest extends PoolTestConfigurator {

    private DoubleWorkProcessor doubleWorkProcessor;
    private ReceivedAttestation attestation;

    @BeforeEach
    void setUp() {
        final ProcessedAttestations processedAttestations = new ProcessedAttestations(spec::hash_tree_root, MAX_PROCESSED_ATTESTATIONS);

        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.of(1, 2, 3));
        attestation = new ReceivedAttestation(sender, message);
        final boolean added = processedAttestations.add(attestation);
        assertThat(added).isTrue();

        doubleWorkProcessor = new DoubleWorkProcessor(processedAttestations, schedulers, source);
        assertThat(doubleWorkProcessor).isNotNull();

        doubleWorkProcessor.subscribe(s -> {
            assertThat(s.getSender())
                    .isNotNull()
                    .isEqualTo(sender);

            assertThat(s.getMessage())
                    .isNotNull()
                    .isEqualTo(message);
        });
    }

    @Test
    void testAddAttestation() {
        source.onNext(attestation);

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
