package org.ethereum.beacon.chain;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.junit.Test;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SlotTickerTests {
  public static final int MILLIS_IN_SECOND = 1000;
  SlotTicker slotTicker;
  long genesisSlot;

  public SlotTickerTests() throws InterruptedException {
    MutableBeaconState beaconState = BeaconState.getEmpty().createMutableCopy();
    while (System.currentTimeMillis() % MILLIS_IN_SECOND < 100
        || System.currentTimeMillis() % MILLIS_IN_SECOND > 900) {
      Thread.sleep(100);
    }
    beaconState.setGenesisTime(Time.of(System.currentTimeMillis() / MILLIS_IN_SECOND));
    ChainSpec chainSpec = new ChainSpecOverride();
    SpecHelpers specHelpers = new SpecHelpers(chainSpec);
    genesisSlot = specHelpers.getChainSpec().getGenesisSlot().longValue();
    slotTicker = new SlotTicker(specHelpers, beaconState);
  }

  @Test
  public void testSlotTicker() throws Exception {
    slotTicker.start();
    Thread.sleep(2000);
    final AtomicBoolean first = new AtomicBoolean(true);
    final CountDownLatch bothAssertsRun = new CountDownLatch(2);
    Flux.from(slotTicker.getTickerStream())
        .subscribe(
            slotNumber -> {
              if (first.get()) {
                assertEquals(SlotNumber.of(genesisSlot + 2), slotNumber);
                bothAssertsRun.countDown();
                first.set(false);
              } else { // second
                assertEquals(SlotNumber.of(genesisSlot + 3), slotNumber);
                bothAssertsRun.countDown();
              }
            });
    assertTrue(
        String.format("%s assertion(s) was not correct or not tested", bothAssertsRun.getCount()),
        bothAssertsRun.await(2, TimeUnit.SECONDS));
  }

  class ChainSpecOverride implements ChainSpec {
    @Override
    public Address getDepositContractAddress() {
      return null;
    }

    @Override
    public UInt64 getDepositContractTreeDepth() {
      return null;
    }

    @Override
    public Gwei getMinDeposit() {
      return null;
    }

    @Override
    public Gwei getMaxDeposit() {
      return null;
    }

    @Override
    public long getEth1FollowDistance() {
      return 0;
    }

    @Override
    public UInt64 getGenesisForkVersion() {
      return null;
    }

    @Override
    public SlotNumber getGenesisSlot() {
      return SlotNumber.of(12345);
    }

    @Override
    public ShardNumber getGenesisStartShard() {
      return null;
    }

    @Override
    public SlotNumber getFarFutureSlot() {
      return null;
    }

    @Override
    public BLSSignature getEmptySignature() {
      return null;
    }

    @Override
    public Bytes1 getBlsWithdrawalPrefixByte() {
      return null;
    }

    @Override
    public int getMaxProposerSlashings() {
      return 0;
    }

    @Override
    public int getMaxCasperSlashings() {
      return 0;
    }

    @Override
    public int getMaxAttestations() {
      return 0;
    }

    @Override
    public int getMaxDeposits() {
      return 0;
    }

    @Override
    public int getMaxExits() {
      return 0;
    }

    @Override
    public ShardNumber getShardCount() {
      return null;
    }

    @Override
    public ValidatorIndex getTargetCommitteeSize() {
      return null;
    }

    @Override
    public Gwei getEjectionBalance() {
      return null;
    }

    @Override
    public UInt64 getMaxBalanceChurnQuotient() {
      return null;
    }

    @Override
    public ShardNumber getBeaconChainShardNumber() {
      return null;
    }

    @Override
    public int getMaxCasperVotes() {
      return 0;
    }

    @Override
    public SlotNumber getLatestBlockRootsLength() {
      return null;
    }

    @Override
    public EpochNumber getLatestRandaoMixesLength() {
      return null;
    }

    @Override
    public EpochNumber getLatestPenalizedExitLength() {
      return null;
    }

    @Override
    public UInt64 getMaxWithdrawalsPerEpoch() {
      return null;
    }

    @Override
    public UInt64 getBaseRewardQuotient() {
      return null;
    }

    @Override
    public UInt64 getWhistleblowerRewardQuotient() {
      return null;
    }

    @Override
    public UInt64 getIncluderRewardQuotient() {
      return null;
    }

    @Override
    public UInt64 getInactivityPenaltyQuotient() {
      return null;
    }

    @Override
    public Time getSlotDuration() {
      return Time.of(1);
    }

    @Override
    public SlotNumber getMinAttestationInclusionDelay() {
      return null;
    }

    @Override
    public SlotNumber.EpochLength getEpochLength() {
      return null;
    }

    @Override
    public SlotNumber getSeedLookahead() {
      return null;
    }

    @Override
    public SlotNumber getEntryExitDelay() {
      return null;
    }

    @Override
    public SlotNumber getEth1DataVotingPeriod() {
      return null;
    }

    @Override
    public SlotNumber getMinValidatorWithdrawalTime() {
      return null;
    }
  }
}
