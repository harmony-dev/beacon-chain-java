package org.ethereum.beacon.wire;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.reactivestreams.Publisher;

/**
 * Represents asynchronous wire interface for subscription-like messages
 */
public interface WireApiSub {

  /**
   * Sends a new block to remote peer(s)
   */
  void sendProposedBlock(BeaconBlock block);

  /**
   * Sends a new attestation to remote peer(s)
   */
  void sendAttestation(Attestation attestation);

  /**
   * Stream of new blocks from remote peer(s)
   * This stream must be distinct, i.e. doesn't contain duplicate blocks
   */
  Publisher<BeaconBlock> inboundBlocksStream();

  /**
   * Stream of new attestations from remote peer(s)
   * This stream must be distinct, i.e. doesn't contain duplicate attestations
   */
  Publisher<Attestation> inboundAttestationsStream();
}
