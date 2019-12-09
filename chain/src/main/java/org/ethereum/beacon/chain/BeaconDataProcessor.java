package org.ethereum.beacon.chain;

import java.util.function.Consumer;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.Time;

public interface BeaconDataProcessor {

  void onTick(Time time);

  boolean onBlock(BeaconBlock block);

  void onAttestation(Attestation attestation);

  void subscribeToStates(Consumer<ObservableBeaconState> subscriber);

  void subscribeToBlocks(Consumer<BeaconBlock> subscriber);
}
