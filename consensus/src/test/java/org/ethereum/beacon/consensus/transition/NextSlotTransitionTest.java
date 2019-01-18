package org.ethereum.beacon.consensus.transition;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.pow.DepositContract;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class NextSlotTransitionTest {

  @Test
  public void test1() {
    Random rnd = new Random();
    UInt64 genesisTime = UInt64.random(rnd);
    Hash32 receiptRoot = Hash32.random(rnd);
    ChainSpec chainSpec = ChainSpec.DEFAULT;

    List<Deposit> deposits = new ArrayList<>();
    for (int i = 0; i < 8000; i++) {
      Deposit deposit = new Deposit(new Hash32[]{Hash32.random(rnd)}, UInt64.ZERO,
          new DepositData(
              new DepositInput(
                  Bytes48.intToBytes48(i), Hash32.random(rnd), Hash32.ZERO, Hash32.ZERO, Bytes96.ZERO
              ), chainSpec.getMaxDeposit().toGWei(), UInt64.ZERO));
      deposits.add(deposit);
    }

    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new DepositContract() {
              @Override
              public ChainStart getChainStart() {
                return new ChainStart(genesisTime, receiptRoot);
              }

              @Override
              public List<Deposit> getInitialDeposits() {
                return deposits;
              }
            }, new SpecHelpers(chainSpec));

    BeaconStateEx initialState =
        initialStateTransition.apply(BeaconBlocks.createGenesis(chainSpec));
    BeaconStateEx s1State = new NextSlotTransition(chainSpec).apply(null, initialState);
    BeaconStateEx s2State = new NextSlotTransition(chainSpec).apply(null, s1State);
    BeaconStateEx s3State = new NextSlotTransition(chainSpec).apply(null, s2State);

    Assert.assertEquals(UInt64.valueOf(3), s3State.getCanonicalState().getSlot());
  }
}
