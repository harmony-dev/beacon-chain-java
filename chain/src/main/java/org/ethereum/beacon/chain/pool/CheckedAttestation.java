package org.ethereum.beacon.chain.pool;

/** A simple DTO that carries an attestation and a result of some check run against it. */
public class CheckedAttestation {
  private final boolean passed;
  private final ReceivedAttestation attestation;

  public CheckedAttestation(boolean passed, ReceivedAttestation attestation) {
    this.passed = passed;
    this.attestation = attestation;
  }

  public boolean isPassed() {
    return passed;
  }

  public ReceivedAttestation getAttestation() {
    return attestation;
  }
}
