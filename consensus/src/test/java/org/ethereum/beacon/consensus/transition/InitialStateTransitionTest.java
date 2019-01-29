package org.ethereum.beacon.consensus.transition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialStateTransitionTest {

  @Test
  public void handleChainStartCorrectly() {
    Random rnd = new Random();
    UInt64 genesisTime = UInt64.random(rnd);
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));

    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, Collections.emptyList()),
            new SpecHelpers(ChainSpec.DEFAULT));

    BeaconState initialState =
        initialStateTransition.apply(
            BeaconBlocks.createGenesis(ChainSpec.DEFAULT)).getCanonicalState();

    assertThat(initialState.getGenesisTime()).isEqualTo(genesisTime);
    assertThat(initialState.getLatestEth1Data()).isEqualTo(eth1Data);
  }
}
