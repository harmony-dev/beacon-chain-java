package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.pool.PoolTestConfigurator;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.impl.SSZBeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.DirectProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.ethereum.beacon.chain.pool.AttestationPool.MAX_ATTESTATION_LOOKAHEAD;
import static org.ethereum.beacon.chain.pool.AttestationPool.MAX_UNKNOWN_ATTESTATIONS;

class IdentificationProcessorTest extends PoolTestConfigurator {

    private IdentificationProcessor identificationProcessor;
    private final DirectProcessor<SlotNumber> newSlots = DirectProcessor.create();
    private final DirectProcessor<BeaconBlock> importedBlocks = DirectProcessor.create();

    @BeforeEach
    void setUp() {
        final InMemoryDatabase db = new InMemoryDatabase();
        final BeaconChainStorage beaconChainStorage =
                new SSZBeaconChainStorageFactory(
                        spec.getObjectHasher(), SerializerFactory.createSSZ(specConstants))
                        .create(db);
        final UnknownAttestationPool unknownAttestationPool = new UnknownAttestationPool(beaconChainStorage.getBlockStorage(), spec, MAX_ATTESTATION_LOOKAHEAD, MAX_UNKNOWN_ATTESTATIONS);
        identificationProcessor = new IdentificationProcessor(unknownAttestationPool, schedulers, source, newSlots, importedBlocks);
        newSlots.onNext(SlotNumber.of(500));

        final MutableBeaconChain beaconChain = createBeaconChain(spec, perSlotTransition, schedulers);
        beaconChain.init();
        final BeaconTuple recentlyProcessed = beaconChain.getRecentlyProcessed();
        final BeaconTuple aTuple = createBlock(recentlyProcessed, spec,
                schedulers.getCurrentTime(), perSlotTransition);
        final BeaconBlock aBlock = aTuple.getBlock();
        importedBlocks.onNext(aBlock);
    }

    @Test
    void testPublishValidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        source.onNext(attestation);
    }
}
