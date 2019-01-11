package org.ethereum.beacon.consensus.verifier;

/**
 * Helper class to hold verification result.
 *
 * <p>Basically, there are two meaningful arguments for this helper. A flag that denotes whether
 * verification has been successful or not. And a message which contains verification details and
 * depends on verifier implementation.
 *
 * @see BeaconBlockVerifier
 * @see BeaconStateVerifier
 */
public class VerificationResult {

  /** Successfully passed verification. */
  public static final VerificationResult PASSED = new VerificationResult("", true);

  private final String message;
  private final boolean passed;

  public VerificationResult(String message, boolean passed) {
    this.message = message;
    this.passed = passed;
  }

  /**
   * Factory method that creates result of unsuccessful verification with given message.
   *
   * @param format a message format which is passed to {@link String#format(String, Object...)}.
   * @param args args to {@link String#format(String, Object...)}
   * @return failed result with given message
   */
  public static VerificationResult createdFailed(String format, Object... args) {
    return new VerificationResult(String.format(format, args), false);
  }

  public boolean isPassed() {
    return passed;
  }

  public String getMessage() {
    return message;
  }
}
