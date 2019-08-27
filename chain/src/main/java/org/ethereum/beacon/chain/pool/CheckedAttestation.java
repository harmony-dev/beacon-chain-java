package org.ethereum.beacon.chain.pool;

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
