package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.chain.storage.BeaconBlockStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxSink;

public class IdentifyProcessor extends AbstractDelegateProcessor<Input, ReceivedAttestation> {

  private final UnknownAttestationPool pool;
  private final DirectProcessor<ReceivedAttestation> unknownAttestations = DirectProcessor.create();
  private final FluxSink<ReceivedAttestation> unknownOut = unknownAttestations.sink();

  public IdentifyProcessor(BeaconBlockStorage blockStorage, BeaconChainSpec spec) {
    this.pool = new UnknownAttestationPool(blockStorage, spec);
  }

  @Override
  protected void hookOnNext(Input value) {
    if (value.getType().equals(BeaconBlock.class)) {
      // forward attestations identified with a new block
      pool.feedNewImportedBlock(value.unbox()).forEach(this::publishOut);
    } else if (value.getType().equals(SlotNumber.class)) {
      pool.feedNewSlot(value.unbox());
    } else if (value.getType().equals(ReceivedAttestation.class)) {
      if (!pool.add(value.unbox())) {
        // forward attestations not added to the pool
        publishOut(value.unbox());
      } else {
        // expose not yet identified attestations
        unknownOut.next(value.unbox());
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported input type: " + value.getType().getSimpleName());
    }
  }

  public DirectProcessor<ReceivedAttestation> getUnknownAttestations() {
    return unknownAttestations;
  }
}
