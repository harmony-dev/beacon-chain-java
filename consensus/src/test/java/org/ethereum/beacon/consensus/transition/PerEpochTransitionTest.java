package org.ethereum.beacon.consensus.transition;

import java.util.List;
import java.util.Random;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class PerEpochTransitionTest {

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
        new InitialStateTransition(new ChainStart(genesisTime, eth1Data, deposits), spec);

    BeaconStateEx[] states = new BeaconStateExImpl[9];

    states[0] = initialStateTransition.apply(spec.get_empty_block());
    for (int i = 1; i < 9; i++) {
      BeaconStateEx cachedState = new StateCachingTransition(spec).apply(states[i - 1]);
      states[i] = new PerSlotTransition(spec).apply(cachedState);
    }
    PerEpochTransition perEpochTransition = new PerEpochTransition(spec);
    BeaconStateEx cachedState = new StateCachingTransition(spec).apply(states[8]);
    BeaconStateEx epochState = perEpochTransition.apply(cachedState);

    // check validators penalized for inactivity
    for (int i = 0; i < deposits.size(); i++) {
      Gwei balanceBefore =
          states[0].getValidatorBalances().get(ValidatorIndex.of(i));
      Gwei balanceAfter =
          epochState.getValidatorBalances().get(ValidatorIndex.of(i));
      Assert.assertTrue(balanceAfter.less(balanceBefore));
    }
  }
}
