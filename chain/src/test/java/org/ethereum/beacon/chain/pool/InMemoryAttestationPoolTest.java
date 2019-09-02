package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.SlotNumber;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

class InMemoryAttestationPoolTest {

    private Publisher<ReceivedAttestation> source = new DefaultTestPublisher<>();
    private Publisher<SlotNumber> newSlots = new DefaultTestPublisher<>();
    private Publisher<Checkpoint> finalizedCheckpoints = new DefaultTestPublisher<>();
    private Publisher<BeaconBlock> importedBlocks = new DefaultTestPublisher<>();
    private Publisher<BeaconBlock> chainHeads = new DefaultTestPublisher<>();

    @Test
    void integrationTest() {
        final AttestationPool pool = new InMemoryAttestationPool()
    }

}
