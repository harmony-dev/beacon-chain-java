package org.ethereum.beacon.validator.attester;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.mockito.Mockito;

public class BeaconChainAttesterTestUtil {

  public static BeaconChainAttesterImpl mockAttester(SpecHelpers specHelpers) {
    return Mockito.spy(new BeaconChainAttesterImpl(specHelpers, specHelpers.getChainSpec()));
  }
}
