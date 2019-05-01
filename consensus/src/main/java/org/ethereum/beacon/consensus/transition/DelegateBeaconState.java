package org.ethereum.beacon.consensus.transition;

import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Eth1Data;
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
import org.ethereum.beacon.ssz.incremental.UpdateListener;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

public class DelegateBeaconState implements BeaconState {
  private final BeaconState delegate;

  public DelegateBeaconState(BeaconState delegate) {
    this.delegate = delegate;
  }

  public BeaconState getDelegate() {
    return delegate;
  }

  @Override
  public UpdateListener getUpdateListener(String observerId,
      Supplier<UpdateListener> listenerFactory) {
    return delegate.getUpdateListener(observerId, listenerFactory);
  }

  @Override
  public Map<String, UpdateListener> getAllUpdateListeners() {
    return delegate.getAllUpdateListeners();
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
  public ReadList<ValidatorIndex, Gwei> getBalances() {
    return delegate.getBalances();
  }

  @Override
  public ReadVector<EpochNumber, Hash32> getLatestRandaoMixes() {
    return delegate.getLatestRandaoMixes();
  }

  @Override
  public ShardNumber getLatestStartShard() {
    return delegate.getLatestStartShard();
  }

  @Override
  public ReadList<Integer, PendingAttestation> getPreviousEpochAttestations() {
    return delegate.getPreviousEpochAttestations();
  }

  @Override
  public ReadList<Integer, PendingAttestation> getCurrentEpochAttestations() {
    return delegate.getCurrentEpochAttestations();
  }

  @Override
  public EpochNumber getPreviousJustifiedEpoch() {
    return delegate.getPreviousJustifiedEpoch();
  }

  @Override
  public EpochNumber getCurrentJustifiedEpoch() {
    return delegate.getCurrentJustifiedEpoch();
  }

  @Override
  public Hash32 getPreviousJustifiedRoot() {
    return delegate.getPreviousJustifiedRoot();
  }

  @Override
  public Hash32 getCurrentJustifiedRoot() {
    return delegate.getCurrentJustifiedRoot();
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
  public Hash32 getFinalizedRoot() {
    return delegate.getFinalizedRoot();
  }

  @Override
  public ReadVector<ShardNumber, Crosslink> getPreviousCrosslinks() {
    return delegate.getPreviousCrosslinks();
  }

  @Override
  public ReadVector<ShardNumber, Crosslink> getCurrentCrosslinks() {
    return delegate.getCurrentCrosslinks();
  }

  @Override
  public ReadVector<SlotNumber, Hash32> getLatestBlockRoots() {
    return delegate.getLatestBlockRoots();
  }

  @Override
  public ReadVector<SlotNumber, Hash32> getLatestStateRoots() {
    return delegate.getLatestStateRoots();
  }

  @Override
  public ReadVector<EpochNumber, Hash32> getLatestActiveIndexRoots() {
    return delegate.getLatestActiveIndexRoots();
  }

  @Override
  public ReadVector<EpochNumber, Gwei> getLatestSlashedBalances() {
    return delegate.getLatestSlashedBalances();
  }

  @Override
  public BeaconBlockHeader getLatestBlockHeader() {
    return delegate.getLatestBlockHeader();
  }

  @Override
  public ReadList<Integer, Hash32> getHistoricalRoots() {
    return delegate.getHistoricalRoots();
  }

  @Override
  public Eth1Data getLatestEth1Data() {
    return delegate.getLatestEth1Data();
  }

  @Override
  public ReadList<Integer, Eth1Data> getEth1DataVotes() {
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
