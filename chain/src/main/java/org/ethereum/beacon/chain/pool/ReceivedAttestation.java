package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.types.p2p.NodeId;

/** An attestation received from the wire. */
public class ReceivedAttestation {

  /** An id of a node sent this attestation. */
  private final NodeId sender;
  /** An attestation message itself. */
  private final Attestation message;

  public ReceivedAttestation(NodeId sender, Attestation message) {
    this.sender = sender;
    this.message = message;
  }

  public ReceivedAttestation(Attestation message) {
    this(null, message);
  }

  public NodeId getSender() {
    return sender;
  }

  public Attestation getMessage() {
    return message;
  }
}
