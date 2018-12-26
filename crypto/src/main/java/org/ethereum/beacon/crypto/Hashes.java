package org.ethereum.beacon.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Utility methods to calculate message hashes */
public abstract class Hashes {
  private Hashes() {}

  private static final BouncyCastleProvider PROVIDER;

  private static final String KECCAK256 = "KECCAK-256";
  private static final String KECCAK512 = "KECCAK-512";

  /** Size of keccak384 in bytes */
  private static final int KECCAK384_SIZE = 384 >> 3;

  static {
    Security.addProvider(PROVIDER = new BouncyCastleProvider());
  }

  /**
   * A low level method that calculates hash using give algorithm.
   *
   * @param input a message.
   * @param algorithm an algorithm.
   * @return the hash.
   */
  private static byte[] digestUsingAlgorithm(BytesValue input, String algorithm) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm, PROVIDER);
      input.update(digest);
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Calculates keccak256 hash.
   *
   * @param input input message.
   * @return the hash.
   */
  public static Bytes32 keccack256(BytesValue input) {
    byte[] output = digestUsingAlgorithm(input, KECCAK256);
    return Bytes32.wrap(output);
  }

  /**
   * Calculates keccak384 by using first 384 bits of {@code KECCAK-512} output.
   *
   * @param input input message.
   * @return the hash.
   */
  public static BytesValue keccack384(BytesValue input) {
    byte[] output = digestUsingAlgorithm(input, KECCAK512);
    return BytesValue.wrap(output, 0, KECCAK384_SIZE);
  }
}
