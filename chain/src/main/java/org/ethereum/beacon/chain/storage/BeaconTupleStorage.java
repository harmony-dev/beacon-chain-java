package org.ethereum.beacon.chain.storage;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import tech.pegasys.artemis.ethereum.core.Hash32;

import javax.annotation.Nonnull;

public class BeaconTupleStorage implements Hash32KeyStorage<BeaconTuple> {

  BeaconBlockStorage blockStorage;
  BeaconStateStorage stateStorage;

  @Override
  public Optional<BeaconTuple> get(Hash32 hash) {
    return blockStorage.get(hash).map(block ->
        stateStorage.get(hash)
            .map(state -> BeaconTuple.of(block, state))
            .orElseThrow(() -> new IllegalStateException("State inconsistency for block "+ block))
    );
  }

  @Override
  public void put(Hash32 hash, BeaconTuple tuple) {
    assert hash.equals(tuple.getHash());

    blockStorage.put(tuple.getBlock());
    stateStorage.put(tuple.getState());
  }

  @Override
  public void remove(@Nonnull Hash32 key) {
    blockStorage.remove(key);
    stateStorage.remove(key);
  }

  @Override
  public void flush() {
    blockStorage.flush();
    stateStorage.flush();
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
