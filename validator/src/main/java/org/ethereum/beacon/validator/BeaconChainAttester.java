package org.ethereum.beacon.validator;

import java.util.List;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public interface BeaconChainAttester {

  Attestation attest(
      UInt24 index,
      List<UInt24> committee,
      UInt64 shard,
      ObservableBeaconState observableState,
      MessageSigner<Bytes96> signer);
}
