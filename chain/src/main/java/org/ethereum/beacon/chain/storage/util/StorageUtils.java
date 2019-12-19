package org.ethereum.beacon.chain.storage.util;

import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import tech.pegasys.artemis.ethereum.core.Hash32;

/** Utility functions to initialize storage from an initial state. */
public class StorageUtils {
  /**
   * Creates a BeaconTuple consisting of the initialState and corresponding block. Currently, the
   * block is empty, but could be re-constructed from the state's block header, in general.
   */
  public static BeaconTuple createInitialBeaconTuple(
      BeaconChainSpec spec, BeaconStateEx initialState) {
    BeaconBlock initialGenesis = spec.get_empty_block();
    Hash32 initialStateRoot = spec.hash_tree_root(initialState);
    BeaconBlock genesis = initialGenesis.withStateRoot(initialStateRoot);
    return BeaconTuple.of(new SignedBeaconBlock(genesis, BLSSignature.ZERO), initialState);
  }

  /**
   * An utility to properly initialize a storage with a specified initial state. Supports only
   * initial state currently. Could be extended in theory, to support finalized states.
   */
  public static void initializeStorage(
      BeaconChainStorage storage, BeaconChainSpec spec, BeaconStateEx initialState) {
    assert storage.getTupleStorage().isEmpty();
    BeaconTuple tuple = createInitialBeaconTuple(spec, initialState);
    Hash32 genesisRoot = spec.hash_tree_root(tuple.getBlock());
    storage.getStateStorage().put(tuple.getBlock().getMessage().getStateRoot(), tuple.getState());
    storage.getBlockStorage().put(genesisRoot, tuple.getBlock());
    Checkpoint justifiedChkpt =
        new Checkpoint(initialState.getCurrentJustifiedCheckpoint().getEpoch(), genesisRoot);
    storage.getJustifiedStorage().set(justifiedChkpt);
    storage.getBestJustifiedStorage().set(justifiedChkpt);
    storage
        .getFinalizedStorage()
        .set(new Checkpoint(initialState.getFinalizedCheckpoint().getEpoch(), genesisRoot));
  }
}
