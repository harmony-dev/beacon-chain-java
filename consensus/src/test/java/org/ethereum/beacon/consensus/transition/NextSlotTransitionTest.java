package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.pow.DepositContract;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class NextSlotTransitionTest {

  @Test
  public void test1() {
    Random rnd = new Random();
    UInt64 genesisTime = UInt64.random(rnd);
    Hash32 receiptRoot = Hash32.random(rnd);

    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new DepositContract() {
              @Override
              public ChainStart getChainStart() {
                return new ChainStart(genesisTime, receiptRoot);
              }

              @Override
              public List<Deposit> getInitialDeposits() {
                return Collections.emptyList();
              }
            });

    BeaconState initialState =
        initialStateTransition.apply(BeaconBlocks.createGenesis(), BeaconState.EMPTY);
    BeaconState s1State = new NextSlotTransition().apply(null, initialState);
    BeaconState s2State = new NextSlotTransition().apply(null, s1State);
    BeaconState s3State = new NextSlotTransition().apply(null, s2State);

    Assert.assertEquals(UInt64.valueOf(3), s3State.getSlot());
  }
}
