package org.ethereum.beacon.consensus;

import org.ethereum.beacon.consensus.spec.ForkChoice.LatestMessage;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import java.util.Optional;
import java.util.function.Function;
import org.ethereum.beacon.core.types.ValidatorIndex;

/** Head function updates head */
public interface HeadFunction {

  /**
   * Updates actual head on chain and returns it
   *
   * @param latestMessageStorage Storage "ValidatorIndex : LatestMessage" at latest state
   * @return head block
   */
  BeaconBlock getHead(Function<ValidatorIndex, Optional<LatestMessage>> latestMessageStorage);
}
