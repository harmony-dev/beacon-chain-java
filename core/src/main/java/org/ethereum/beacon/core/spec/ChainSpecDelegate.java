package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.types.Ether;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class ChainSpecDelegate implements ChainSpec {

  private final ChainSpec delegate;

  public ChainSpecDelegate(ChainSpec delegate) {
    this.delegate = delegate;
  }

  @Override
  public Address getDepositContractAddress() {
    return delegate.getDepositContractAddress();
  }

  @Override
  public UInt64 getDepositContractTreeDepth() {
    return delegate.getDepositContractTreeDepth();
  }

  @Override
  public Ether getMinDeposit() {
    return delegate.getMinDeposit();
  }

  @Override
  public Ether getMaxDeposit() {
    return delegate.getMaxDeposit();
  }

  @Override
  public long getEth1FollowDistance() {
    return delegate.getEth1FollowDistance();
  }

  @Override
  public UInt64 getGenesisForkVersion() {
    return delegate.getGenesisForkVersion();
  }

  @Override
  public UInt64 getGenesisSlot() {
    return delegate.getGenesisSlot();
  }

  @Override
  public UInt64 getGenesisStartShard() {
    return delegate.getGenesisStartShard();
  }

  @Override
  public UInt64 getFarFutureSlot() {
    return delegate.getFarFutureSlot();
  }

  @Override
  public Bytes96 getEmptySignature() {
    return delegate.getEmptySignature();
  }

  @Override
  public Bytes1 getBlsWithdrawalPrefixByte() {
    return delegate.getBlsWithdrawalPrefixByte();
  }

  @Override
  public int getMaxProposerSlashings() {
    return delegate.getMaxProposerSlashings();
  }

  @Override
  public int getMaxCasperSlashings() {
    return delegate.getMaxCasperSlashings();
  }

  @Override
  public int getMaxAttestations() {
    return delegate.getMaxAttestations();
  }

  @Override
  public int getMaxDeposits() {
    return delegate.getMaxDeposits();
  }

  @Override
  public int getMaxExits() {
    return delegate.getMaxExits();
  }

  @Override
  public UInt64 getShardCount() {
    return delegate.getShardCount();
  }

  @Override
  public UInt24 getTargetCommitteeSize() {
    return delegate.getTargetCommitteeSize();
  }

  @Override
  public Ether getEjectionBalance() {
    return delegate.getEjectionBalance();
  }

  @Override
  public UInt64 getMaxBalanceChurnQuotient() {
    return delegate.getMaxBalanceChurnQuotient();
  }

  @Override
  public UInt64 getBeaconChainShardNumber() {
    return delegate.getBeaconChainShardNumber();
  }

  @Override
  public int getMaxCasperVotes() {
    return delegate.getMaxCasperVotes();
  }

  @Override
  public UInt64 getLatestBlockRootsLength() {
    return delegate.getLatestBlockRootsLength();
  }

  @Override
  public UInt64 getLatestRandaoMixesLength() {
    return delegate.getLatestRandaoMixesLength();
  }

  @Override
  public UInt64 getLatestPenalizedExitLength() {
    return delegate.getLatestPenalizedExitLength();
  }

  @Override
  public UInt64 getMaxWithdrawalsPerEpoch() {
    return delegate.getMaxWithdrawalsPerEpoch();
  }

  @Override
  public UInt64 getBaseRewardQuotient() {
    return delegate.getBaseRewardQuotient();
  }

  @Override
  public UInt64 getWhistleblowerRewardQuotient() {
    return delegate.getWhistleblowerRewardQuotient();
  }

  @Override
  public UInt64 getIncluderRewardQuotient() {
    return delegate.getIncluderRewardQuotient();
  }

  @Override
  public UInt64 getInactivityPenaltyQuotient() {
    return delegate.getInactivityPenaltyQuotient();
  }

  @Override
  public UInt64 getSlotDuration() {
    return delegate.getSlotDuration();
  }

  @Override
  public UInt64 getMinAttestationInclusionDelay() {
    return delegate.getMinAttestationInclusionDelay();
  }

  @Override
  public UInt64 getEpochLength() {
    return delegate.getEpochLength();
  }

  @Override
  public UInt64 getSeedLookahead() {
    return delegate.getSeedLookahead();
  }

  @Override
  public UInt64 getEntryExitDelay() {
    return delegate.getEntryExitDelay();
  }

  @Override
  public UInt64 getEth1DataVotingPeriod() {
    return delegate.getEth1DataVotingPeriod();
  }

  @Override
  public UInt64 getMinValidatorWithdrawalTime() {
    return delegate.getMinValidatorWithdrawalTime();
  }
}
