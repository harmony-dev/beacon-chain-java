package org.ethereum.beacon.consensus.transition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ethereum.beacon.core.spec.NonConfigurableConstants.SECONDS_PER_DAY;

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
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

public class InitialStateTransitionTest {

  @Test
  public void handleChainStartCorrectly() {
    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
    Random rnd = new Random();
    Time eth1Time = Time.castFrom(UInt64.random(rnd));
    List<Deposit> deposits = Collections.emptyList();
    ReadList<Integer, DepositData> depositDataList =
        ReadList.wrap(
            deposits.stream().map(Deposit::getData).collect(Collectors.toList()),
            Integer::new,
            1L << spec.getConstants().getDepositContractTreeDepth().getIntValue());
    Eth1Data eth1Data = new Eth1Data(
        spec.hash_tree_root(depositDataList), UInt64.ZERO, Hash32.random(rnd));
    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(eth1Time, eth1Data, Collections.emptyList()),
            spec);

    BeaconState initialState =
        initialStateTransition.apply(spec.get_empty_block());

    Time expectedTime = Time.castFrom(
        eth1Time
            .minus(eth1Time.modulo(spec.getConstants().getSecondsPerDay())))
            .plus(spec.getConstants().getSecondsPerDay().times(2));

    assertThat(initialState.getGenesisTime()).isEqualTo(expectedTime);
    assertThat(initialState.getEth1Data()).isEqualTo(Eth1Data.EMPTY);
  }
}
