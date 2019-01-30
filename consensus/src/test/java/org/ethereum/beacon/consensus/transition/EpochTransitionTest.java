package org.ethereum.beacon.consensus.transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters.Impl;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class EpochTransitionTest {

  @Test
  public void test1() {
    Random rnd = new Random();
    UInt64 genesisTime = UInt64.random(rnd);
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));
    ChainSpec chainSpec =
        new ChainSpec.DefaultChainSpec() {
          @Override
          public SlotNumber.EpochLength getEpochLength() {
            return new SlotNumber.EpochLength(UInt64.valueOf(8));
          }
        };

    SpecHelpers specHelpers = new SpecHelpers(chainSpec);

    List<Deposit> deposits = TestUtils.getAnyDeposits(specHelpers, 8).getValue0();

    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, deposits),
            specHelpers);

    BeaconStateEx[] states = new BeaconStateEx[9];

    states[0] = initialStateTransition.apply(BeaconBlocks.createGenesis(chainSpec));
    for (int i = 1; i < 9; i++) {
      states[i] = new NextSlotTransition(chainSpec).apply(null, states[i - 1]);
    }
    EpochTransition epochTransition = new EpochTransition(specHelpers);
    BeaconStateEx epochState = epochTransition.apply(null, states[8]);

    System.out.println(epochState);
  }
}
