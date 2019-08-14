package org.ethereum.beacon.validator.proposer;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.pow.DepositContract;
import org.mockito.Mockito;

public abstract class BeaconChainProposerTestUtil {
  private BeaconChainProposerTestUtil() {}

  public static BeaconChainProposerImpl mockProposer(
      BlockTransition<BeaconStateEx> perBlockTransition,
      DepositContract depositContract,
      BeaconChainSpec spec) {
    return Mockito.spy(
        new BeaconChainProposerImpl(
            spec,
            perBlockTransition,
            depositContract));
  }
}
