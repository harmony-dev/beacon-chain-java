package org.ethereum.beacon.consensus.transition;

import java.util.List;
import java.util.Random;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
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
    ChainSpec chainSpec =
        new ChainSpec() {
          @Override
          public SlotNumber.EpochLength getEpochLength() {
            return new SlotNumber.EpochLength(UInt64.valueOf(8));
          }
        };
    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(chainSpec);

    List<Deposit> deposits = TestUtils.getAnyDeposits(specHelpers, 8).getValue0();

    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, deposits),
            specHelpers);

    BeaconStateEx initialState =
        initialStateTransition.apply(BeaconBlocks.createGenesis(chainSpec));
    BeaconStateEx s1State = new PerSlotTransition(specHelpers).apply(initialState);
    BeaconStateEx s2State = new PerSlotTransition(specHelpers).apply(s1State);
    BeaconStateEx s3State = new PerSlotTransition(specHelpers).apply(s2State);

    Assert.assertEquals(chainSpec.getGenesisSlot().plus(3), s3State.getCanonicalState().getSlot());
  }
}
