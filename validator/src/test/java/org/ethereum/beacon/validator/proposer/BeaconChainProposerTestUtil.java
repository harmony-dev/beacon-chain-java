package org.ethereum.beacon.validator.proposer;

import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.pow.DepositContract;
import org.mockito.Mockito;

public abstract class BeaconChainProposerTestUtil {
  private BeaconChainProposerTestUtil() {}

  public static BeaconChainProposerImpl mockProposer(
      BlockTransition<BeaconState> stateStateTransition,
      DepositContract depositContract,
      SpecHelpers specHelpers) {
    return Mockito.spy(
        new BeaconChainProposerImpl(
            specHelpers, specHelpers.getChainSpec(), stateStateTransition, depositContract));
  }
}
