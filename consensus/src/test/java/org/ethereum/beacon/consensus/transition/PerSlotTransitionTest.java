package org.ethereum.beacon.consensus.transition;

import java.util.List;
import java.util.Random;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class PerSlotTransitionTest {

  @Test
  public void test1() {
    Random rnd = new Random();
    Time genesisTime = Time.castFrom(UInt64.random(rnd));
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));
    SpecConstants specConstants =
        new SpecConstants() {
          @Override
          public SlotNumber.EpochLength getSlotsPerEpoch() {
            return new SlotNumber.EpochLength(UInt64.valueOf(8));
          }
        };
    BeaconChainSpec spec = BeaconChainSpec.createWithSSZHasher(specConstants);

    List<Deposit> deposits = TestUtils.getAnyDeposits(rnd, spec, 8).getValue0();

    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, deposits),
            spec);

    BeaconStateEx initialState =
        initialStateTransition.apply(spec.get_empty_block());
    BeaconStateEx c1State = new StateCachingTransition(spec).apply(initialState);
    BeaconStateEx s1State = new PerSlotTransition(spec).apply(c1State);
    BeaconStateEx c2State = new StateCachingTransition(spec).apply(s1State);
    BeaconStateEx s2State = new PerSlotTransition(spec).apply(c2State);
    BeaconStateEx c3State = new StateCachingTransition(spec).apply(s2State);
    BeaconStateEx s3State = new PerSlotTransition(spec).apply(c3State);

    Assert.assertEquals(specConstants.getGenesisSlot().plus(3), s3State.getSlot());
  }
}
