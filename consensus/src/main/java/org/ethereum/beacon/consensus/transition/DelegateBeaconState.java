package org.ethereum.beacon.consensus.transition;

import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

public class DelegateBeaconState implements BeaconState {
  private final BeaconState delegate;

  public DelegateBeaconState(BeaconState delegate) {
    this.delegate = delegate;
  }

  public static BeaconState getEmpty() {
    return BeaconState.getEmpty();
  }

  public BeaconState getDelegate() {
    return delegate;
  }

  @Override
  public SlotNumber getSlot() {
    return delegate.getSlot();
  }

  @Override
  public Time getGenesisTime() {
    return delegate.getGenesisTime();
  }

  @Override
  public Fork getFork() {
    return delegate.getFork();
  }

  @Override
  public ReadList<ValidatorIndex, ValidatorRecord> getValidatorRegistry() {
    return delegate.getValidatorRegistry();
  }

  @Override
  public ReadList<ValidatorIndex, Gwei> getValidatorBalances() {
    return delegate.getValidatorBalances();
  }

  @Override
  public EpochNumber getValidatorRegistryUpdateEpoch() {
    return delegate.getValidatorRegistryUpdateEpoch();
  }

  @Override
  public ReadList<EpochNumber, Hash32> getLatestRandaoMixes() {
    return delegate.getLatestRandaoMixes();
  }

  @Override
  public ShardNumber getPreviousShufflingStartShard() {
    return delegate.getPreviousShufflingStartShard();
  }

  @Override
  public ShardNumber getCurrentShufflingStartShard() {
    return delegate.getCurrentShufflingStartShard();
  }

  @Override
  public EpochNumber getPreviousShufflingEpoch() {
    return delegate.getPreviousShufflingEpoch();
  }

  @Override
  public EpochNumber getCurrentShufflingEpoch() {
    return delegate.getCurrentShufflingEpoch();
  }

  @Override
  public Hash32 getPreviousShufflingSeed() {
    return delegate.getPreviousShufflingSeed();
  }

  @Override
  public Hash32 getCurrentShufflingSeed() {
    return delegate.getCurrentShufflingSeed();
  }

  @Override
  public EpochNumber getPreviousJustifiedEpoch() {
    return delegate.getPreviousJustifiedEpoch();
  }

  @Override
  public EpochNumber getJustifiedEpoch() {
    return delegate.getJustifiedEpoch();
  }

  @Override
  public Bitfield64 getJustificationBitfield() {
    return delegate.getJustificationBitfield();
  }

  @Override
  public EpochNumber getFinalizedEpoch() {
    return delegate.getFinalizedEpoch();
  }

  @Override
  public ReadList<ShardNumber, Crosslink> getLatestCrosslinks() {
    return delegate.getLatestCrosslinks();
  }

  @Override
  public ReadList<SlotNumber, Hash32> getLatestBlockRoots() {
    return delegate.getLatestBlockRoots();
  }

  @Override
  public ReadList<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return delegate.getLatestActiveIndexRoots();
  }

  @Override
  public ReadList<EpochNumber, Gwei> getLatestSlashedBalances() {
    return delegate.getLatestSlashedBalances();
  }

  @Override
  public ReadList<Integer, PendingAttestation> getLatestAttestations() {
    return delegate.getLatestAttestations();
  }

  @Override
  public ReadList<Integer, Hash32> getBatchedBlockRoots() {
    return delegate.getBatchedBlockRoots();
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return delegate.getLatestEth1Data();
  }

  @Override
  public ReadList<Integer, Eth1DataVote> getEth1DataVotes() {
    return delegate.getEth1DataVotes();
  }

  @Override
  public UInt64 getDepositIndex() {
    return delegate.getDepositIndex();
  }

  @Override
  public MutableBeaconState createMutableCopy() {
    return delegate.createMutableCopy();
  }

  @Override
  public String toStringShort(@Nullable SpecConstants constants) {
    return delegate.toStringShort(constants);
  }
}
