package org.ethereum.beacon.consensus;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import java.util.Optional;
import java.util.function.Function;

/** Head function updates head */
public interface HeadFunction {

  /**
   * Updates actual head on chain and returns it
   *
   * @param latestAttestationStorage Storage "validator : attestation" at latest state
   * @return head block
   */
  BeaconBlock getHead(Function<ValidatorRecord, Optional<Attestation>> latestAttestationStorage);
}
