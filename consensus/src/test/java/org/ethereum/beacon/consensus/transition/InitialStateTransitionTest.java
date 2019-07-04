package org.ethereum.beacon.consensus.transition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialStateTransitionTest {

  @Test
  public void handleChainStartCorrectly() {
    Random rnd = new Random();
    Time genesisTime = Time.castFrom(UInt64.random(rnd));
    List<Deposit> deposits = Collections.emptyList();
    List<DepositData> depositDataList =
        deposits.stream().map(Deposit::getData).collect(Collectors.toList());
    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
    Eth1Data eth1Data = new Eth1Data(
        spec.hash_tree_root(depositDataList), UInt64.ZERO, Hash32.random(rnd));
    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, Collections.emptyList()),
            spec);

    BeaconState initialState =
        initialStateTransition.apply(spec.get_empty_block());

    assertThat(initialState.getGenesisTime()).isEqualTo(genesisTime);
    assertThat(initialState.getEth1Data()).isEqualTo(eth1Data);
  }
}
