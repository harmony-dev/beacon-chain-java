package org.ethereum.beacon.consensus.transition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Random;
import org.ethereum.beacon.consensus.BeaconChainSpec;
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
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), UInt64.ZERO, Hash32.random(rnd));

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, Collections.emptyList()),
            spec);

    BeaconState initialState =
        initialStateTransition.apply(spec.get_empty_block());

    assertThat(initialState.getGenesisTime()).isEqualTo(genesisTime);
    assertThat(initialState.getLatestEth1Data()).isEqualTo(eth1Data);
  }
}
