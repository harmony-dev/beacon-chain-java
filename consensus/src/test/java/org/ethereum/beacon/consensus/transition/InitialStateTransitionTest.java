package org.ethereum.beacon.consensus.transition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.BeaconStateImpl;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.pow.DepositContract;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialStateTransitionTest {

  @Test
  public void handleChainStartCorrectly() {
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
            },
            ChainSpec.DEFAULT);

    BeaconState initialState =
        initialStateTransition.apply(
            BeaconBlocks.createGenesis(ChainSpec.DEFAULT), BeaconStateImpl.EMPTY);

    assertThat(initialState.getGenesisTime()).isEqualTo(genesisTime);
    assertThat(initialState.getLatestDepositRoot()).isEqualTo(receiptRoot);
  }
}
