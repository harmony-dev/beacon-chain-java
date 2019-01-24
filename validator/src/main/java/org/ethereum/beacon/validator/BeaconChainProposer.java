package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;

public interface BeaconChainProposer {

  BeaconBlock propose(
      UInt24 index, ObservableBeaconState observableState, MessageSigner<Bytes96> signer);
}
