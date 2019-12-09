package org.ethereum.beacon.chain.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.ethereum.beacon.consensus.spec.ForkChoice.LatestMessage;
import org.ethereum.beacon.consensus.spec.ForkChoice.Store;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class InMemoryStore implements TransactionalStore {

  private final Map<Hash32, BeaconBlock> blocks = new HashMap<>();
  private final Map<Hash32, BeaconState> blockStates = new HashMap<>();
  private final Map<Checkpoint, BeaconState> checkpointStates = new HashMap<>();
  private final Map<ValidatorIndex, LatestMessage> latestMessages = new HashMap<>();
  private final Map<Hash32, List<Hash32>> children = new HashMap<>();

  private Time genesisTime;
  private Time time;
  private Checkpoint justifiedCheckpoint;
  private Checkpoint finalizedCheckpoint;

  private Checkpoint bestJustifiedCheckpoint;

  @Override
  public Time getTime() {
    return time;
  }

  @Override
  public void setTime(Time time) {
    this.time = time;
  }

  @Override
  public Time getGenesisTime() {
    return genesisTime;
  }

  @Override
  public void setGenesisTime(Time time) {
    this.genesisTime = time;
  }

  @Override
  public Checkpoint getJustifiedCheckpoint() {
    return justifiedCheckpoint;
  }

  @Override
  public void setJustifiedCheckpoint(Checkpoint checkpoint) {
    this.justifiedCheckpoint = checkpoint;
  }

  @Override
  public Checkpoint getBestJustifiedCheckpoint() {
    return bestJustifiedCheckpoint;
  }

  @Override
  public void setBestJustifiedCheckpoint(Checkpoint checkpoint) {
    this.bestJustifiedCheckpoint = checkpoint;
  }

  @Override
  public Checkpoint getFinalizedCheckpoint() {
    return finalizedCheckpoint;
  }

  @Override
  public void setFinalizedCheckpoint(Checkpoint checkpoint) {
    this.finalizedCheckpoint = checkpoint;
  }

  @Override
  public Optional<BeaconBlock> getBlock(Hash32 root) {
    return Optional.ofNullable(blocks.get(root));
  }

  @Override
  public void setBlock(Hash32 root, BeaconBlock block) {
    blocks.put(root, block);
    List<Hash32> children =
        this.children.computeIfAbsent(block.getParentRoot(), parent -> new ArrayList<>());
    children.add(root);
  }

  @Override
  public Optional<BeaconState> getState(Hash32 root) {
    return Optional.ofNullable(blockStates.get(root));
  }

  @Override
  public void setState(Hash32 root, BeaconState state) {
    blockStates.put(root, state);
  }

  @Override
  public Optional<BeaconState> getCheckpointState(Checkpoint checkpoint) {
    return Optional.ofNullable(checkpointStates.get(checkpoint));
  }

  @Override
  public void setCheckpointState(Checkpoint checkpoint, BeaconState state) {
    checkpointStates.put(checkpoint, state);
  }

  @Override
  public Optional<LatestMessage> getLatestMessage(ValidatorIndex index) {
    return Optional.ofNullable(latestMessages.get(index));
  }

  @Override
  public void setLatestMessage(ValidatorIndex index, LatestMessage message) {
    latestMessages.put(index, message);
  }

  @Override
  public List<Hash32> getChildren(Hash32 root) {
    return children.getOrDefault(root, Collections.emptyList());
  }

  @Override
  public StoreTx newTx() {
    return new StoreTxImpl(this);
  }

  private static final class StoreTxImpl implements StoreTx {

    private final Store upstream;

    private final InMemoryStore tx = new InMemoryStore();

    private StoreTxImpl(Store upstream) {
      this.upstream = upstream;

      tx.time = upstream.getTime();
      tx.genesisTime = upstream.getGenesisTime();
      tx.justifiedCheckpoint = upstream.getJustifiedCheckpoint();
      tx.finalizedCheckpoint = upstream.getFinalizedCheckpoint();
      tx.bestJustifiedCheckpoint = upstream.getBestJustifiedCheckpoint();
    }

    @Override
    public void commit() {
      upstream.setTime(tx.getTime());
      upstream.setGenesisTime(tx.getGenesisTime());
      upstream.setJustifiedCheckpoint(tx.getJustifiedCheckpoint());
      upstream.setFinalizedCheckpoint(tx.getFinalizedCheckpoint());
      upstream.setBestJustifiedCheckpoint(tx.getBestJustifiedCheckpoint());

      tx.blocks.forEach(upstream::setBlock);
      tx.blockStates.forEach(upstream::setState);
      tx.checkpointStates.forEach(upstream::setCheckpointState);
      tx.latestMessages.forEach(upstream::setLatestMessage);
    }

    @Override
    public Time getTime() {
      return tx.getTime();
    }

    @Override
    public void setTime(Time time) {
      tx.setTime(time);
    }

    @Override
    public Time getGenesisTime() {
      return tx.getGenesisTime();
    }

    @Override
    public void setGenesisTime(Time time) {
      tx.setGenesisTime(time);
    }

    @Override
    public Checkpoint getJustifiedCheckpoint() {
      return tx.getJustifiedCheckpoint();
    }

    @Override
    public void setJustifiedCheckpoint(Checkpoint checkpoint) {
      tx.setJustifiedCheckpoint(checkpoint);
    }

    @Override
    public Checkpoint getBestJustifiedCheckpoint() {
      return tx.getBestJustifiedCheckpoint();
    }

    @Override
    public void setBestJustifiedCheckpoint(Checkpoint checkpoint) {
      tx.setBestJustifiedCheckpoint(checkpoint);
    }

    @Override
    public Checkpoint getFinalizedCheckpoint() {
      return tx.getFinalizedCheckpoint();
    }

    @Override
    public void setFinalizedCheckpoint(Checkpoint checkpoint) {
      tx.setFinalizedCheckpoint(checkpoint);
    }

    @Override
    public Optional<BeaconBlock> getBlock(Hash32 root) {
      Optional<BeaconBlock> block = tx.getBlock(root);
      if (!block.isPresent()) {
        return upstream.getBlock(root);
      } else {
        return block;
      }
    }

    @Override
    public void setBlock(Hash32 root, BeaconBlock block) {
      tx.setBlock(root, block);
    }

    @Override
    public Optional<BeaconState> getState(Hash32 root) {
      Optional<BeaconState> state = tx.getState(root);
      if (!state.isPresent()) {
        return upstream.getState(root);
      } else {
        return state;
      }
    }

    @Override
    public void setState(Hash32 root, BeaconState state) {
      tx.setState(root, state);
    }

    @Override
    public Optional<BeaconState> getCheckpointState(Checkpoint checkpoint) {
      Optional<BeaconState> state = tx.getCheckpointState(checkpoint);
      if (!state.isPresent()) {
        return upstream.getCheckpointState(checkpoint);
      } else {
        return state;
      }
    }

    @Override
    public void setCheckpointState(Checkpoint checkpoint, BeaconState state) {
      tx.setCheckpointState(checkpoint, state);
    }

    @Override
    public Optional<LatestMessage> getLatestMessage(ValidatorIndex index) {
      Optional<LatestMessage> latestMessage = tx.getLatestMessage(index);
      if (!latestMessage.isPresent()) {
        return upstream.getLatestMessage(index);
      } else {
        return latestMessage;
      }
    }

    @Override
    public void setLatestMessage(ValidatorIndex index, LatestMessage message) {
      tx.setLatestMessage(index, message);
    }

    @Override
    public List<Hash32> getChildren(Hash32 root) {
      Set<Hash32> children = new HashSet<>(upstream.getChildren(root));
      children.addAll(tx.getChildren(root));
      return new ArrayList<>(children);
    }
  }
}
