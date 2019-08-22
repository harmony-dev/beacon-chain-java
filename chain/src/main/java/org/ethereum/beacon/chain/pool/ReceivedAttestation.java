package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.types.p2p.NodeId;

public class ReceivedAttestation {

  private final NodeId sender;
  private final Attestation message;

  public ReceivedAttestation(NodeId sender, Attestation message) {
    this.sender = sender;
    this.message = message;
  }

  public NodeId getSender() {
    return sender;
  }

  public Attestation getMessage() {
    return message;
  }
}
