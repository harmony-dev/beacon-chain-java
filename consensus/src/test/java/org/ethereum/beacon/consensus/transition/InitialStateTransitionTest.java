package org.ethereum.beacon.consensus.transition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Random;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialStateTransitionTest {

  @Test
  public void handleChainStartCorrectly() {
    Random rnd = new Random();
    Time genesisTime = Time.castFrom(UInt64.random(rnd));
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT);
    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, Collections.emptyList()),
            specHelpers);

    BeaconState initialState =
        initialStateTransition.apply(
            BeaconBlocks.createGenesis(SpecConstants.DEFAULT));

    assertThat(initialState.getGenesisTime()).isEqualTo(genesisTime);
    assertThat(initialState.getLatestEth1Data()).isEqualTo(eth1Data);
  }
}
