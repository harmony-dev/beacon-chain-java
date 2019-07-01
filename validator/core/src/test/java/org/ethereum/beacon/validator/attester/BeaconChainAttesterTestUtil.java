package org.ethereum.beacon.validator.attester;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.mockito.Mockito;

public class BeaconChainAttesterTestUtil {

  public static BeaconChainAttesterImpl mockAttester(BeaconChainSpec spec) {
    return Mockito.spy(new BeaconChainAttesterImpl(spec));
  }
}
