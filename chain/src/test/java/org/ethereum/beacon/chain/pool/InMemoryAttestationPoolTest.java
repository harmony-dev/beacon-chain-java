package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.impl.*;
import org.ethereum.beacon.consensus.*;
import org.ethereum.beacon.consensus.transition.*;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.*;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

class InMemoryAttestationPoolTest {

    private Publisher<ReceivedAttestation> source = Flux.empty();
    private Publisher<SlotNumber> newSlots = Flux.empty();
    private Publisher<Checkpoint> finalizedCheckpoints = Flux.empty();
    private Publisher<BeaconBlock> importedBlocks = Flux.empty();
    private Publisher<BeaconBlock> chainHeads = Flux.empty();
    private Schedulers schedulers = Schedulers.createDefault();

    private SpecConstants specConstants =
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
    private BeaconChainSpec spec = BeaconChainSpec.createWithDefaultHasher(specConstants);
    private InMemoryDatabase db = new InMemoryDatabase();
    private BeaconChainStorage beaconChainStorage =
            new SSZBeaconChainStorageFactory(
                    spec.getObjectHasher(), SerializerFactory.createSSZ(specConstants))
                    .create(db);

    private PerEpochTransition perEpochTransition = new PerEpochTransition(spec);
    private StateTransition<BeaconStateEx> perSlotTransition = new PerSlotTransition(spec);
    private EmptySlotTransition slotTransition =
            new EmptySlotTransition(
                    new ExtendedSlotTransition(perEpochTransition, perSlotTransition, spec));

    @Test
    void integrationTest() {
        final AttestationPool pool = AttestationPool.create(
                source,
                newSlots,
                finalizedCheckpoints,
                importedBlocks,
                chainHeads,
                schedulers,
                spec,
                beaconChainStorage,
                slotTransition
        );

        pool.start();
    }

}
