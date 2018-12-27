package org.ethereum.beacon.chain.storage;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconTupleStorage implements Hash32KeyStorage<BeaconTuple> {

  BeaconBlockStorage blockStorage;
  BeaconStateStorage stateStorage;

  @Override
  public Optional<BeaconTuple> get(Hash32 hash) {
    Optional<BeaconBlock> block = blockStorage.get(hash);
    if (block.isPresent()) {
      Optional<BeaconState> state = stateStorage.get(hash);
      checkArgument(state.isPresent(), "State inconsistency for block %s", block);
      return Optional.of(BeaconTuple.create(block.get(), state.get()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void put(Hash32 hash, BeaconTuple tuple) {
    assert hash.equals(tuple.getHash());

    blockStorage.put(tuple.getBlock());
    stateStorage.put(tuple.getState());
  }

  public Optional<BeaconTuple> getCanonicalHead() {
    Optional<BeaconBlock> canonicalHead = blockStorage.getCanonicalHead();
    if (canonicalHead.isPresent()) {
      return get(canonicalHead.get().getHash());
    } else {
      return Optional.empty();
    }
  }
}
