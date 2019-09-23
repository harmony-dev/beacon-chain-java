package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.AttestationPool;
import org.ethereum.beacon.chain.pool.PoolTestConfigurator;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.checker.SignatureEncodingChecker;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

class SignatureEncodingProcessorTest extends PoolTestConfigurator {

    private SignatureEncodingProcessor signatureEncodingProcessor;

    @BeforeEach
    void setUp() {
        final SignatureEncodingChecker checker = new SignatureEncodingChecker();
        org.ethereum.beacon.schedulers.Scheduler parallelExecutor = schedulers.newParallelDaemon("attestation-pool-%d", AttestationPool.MAX_THREADS);
        signatureEncodingProcessor = new SignatureEncodingProcessor(checker, parallelExecutor, source);
    }

    @Test
    void testValidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        source.onNext(attestation);

        //TODO: assert signatureEncodingProcessor.getValid()
    }

    @Test
    void testInvalidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final AttestationData invalidAttestationData = new AttestationData(
                Hashes.sha256(BytesValue.fromHexString("aa")),
                new Checkpoint(EpochNumber.of(231), Hashes.sha256(BytesValue.fromHexString("bb"))),
                new Checkpoint(EpochNumber.of(2), Hashes.sha256(BytesValue.fromHexString("cc"))),
                Crosslink.EMPTY);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"), invalidAttestationData);
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        source.onNext(attestation);

        //TODO: assert signatureEncodingProcessor.getInvalid()
    }
}
