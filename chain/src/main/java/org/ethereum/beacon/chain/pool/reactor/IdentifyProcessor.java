package org.ethereum.beacon.chain.pool.reactor;

import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.chain.pool.registry.UnknownAttestationPool;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.stream.AbstractDelegateProcessor;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxSink;

/**
 * Processor throttling attestations through {@link UnknownAttestationPool}.
 *
 * <p>Input:
 *
 * <ul>
 *   <li>newly imported blocks
 *   <li>new slots
 *   <li>attestations
 * </ul>
 *
 * <p>Output:
 *
 * <ul>
 *   <li>instantly identified attestations
 *   <li>attestations identified upon a new block come
 * </ul>
 */
public class IdentifyProcessor extends AbstractDelegateProcessor<Object, ReceivedAttestation> {

  private final UnknownAttestationPool pool;
  private final DirectProcessor<ReceivedAttestation> unknownAttestations = DirectProcessor.create();
  private final FluxSink<ReceivedAttestation> unknownOut = unknownAttestations.sink();

  public IdentifyProcessor(UnknownAttestationPool pool) {
    this.pool = pool;
  }

  @Override
  protected void hookOnNext(Object value) {
    if (value.getClass().equals(BeaconBlock.class)) {
      // forward attestations identified with a new block
      pool.feedNewImportedBlock((BeaconBlock) value).forEach(this::publishOut);
    } else if (value.getClass().equals(SlotNumber.class)) {
      pool.feedNewSlot((SlotNumber) value);
    } else if (value.getClass().equals(ReceivedAttestation.class)) {
      if (pool.isInitialized()) {
        return;
      }

      ReceivedAttestation attestation = (ReceivedAttestation) value;

      if (!pool.add(attestation)) {
        // forward attestations not added to the pool
        publishOut(attestation);
      } else {
        // expose not yet identified attestations
        unknownOut.next(attestation);
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported input type: " + value.getClass().getSimpleName());
    }
  }

  public DirectProcessor<ReceivedAttestation> getUnknownAttestations() {
    return unknownAttestations;
  }
}
