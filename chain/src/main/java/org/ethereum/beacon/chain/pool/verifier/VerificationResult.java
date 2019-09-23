package org.ethereum.beacon.chain.pool.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;

/** Result of attestation batch verification. Contains a list of valid and invalid attestations. */
public final class VerificationResult {

  static VerificationResult allInvalid(List<ReceivedAttestation> attestations) {
    return new VerificationResult(Collections.emptyList(), Collections.emptyList(), attestations);
  }

  public static final VerificationResult EMPTY =
      new VerificationResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

  private final List<ReceivedAttestation> valid;
  private final List<IndexedAttestation> validIndexed;
  private final List<ReceivedAttestation> invalid;

  VerificationResult(
      List<ReceivedAttestation> valid,
      List<IndexedAttestation> validIndexed,
      List<ReceivedAttestation> invalid) {
    this.valid = valid;
    this.validIndexed = validIndexed;
    this.invalid = invalid;
  }

  public List<ReceivedAttestation> getValid() {
    return valid;
  }

  public List<IndexedAttestation> getValidIndexed() {
    return validIndexed;
  }

  public List<ReceivedAttestation> getInvalid() {
    return invalid;
  }

  public VerificationResult merge(VerificationResult other) {
    List<ReceivedAttestation> valid = new ArrayList<>(this.valid);
    List<IndexedAttestation> validIndexed = new ArrayList<>(this.validIndexed);
    List<ReceivedAttestation> invalid = new ArrayList<>(this.invalid);
    valid.addAll(other.valid);
    validIndexed.addAll(other.validIndexed);
    invalid.addAll(other.invalid);

    return new VerificationResult(valid, validIndexed, invalid);
  }
}
