package org.ethereum.beacon.crypto;

import java.security.MessageDigest;
import java.security.Security;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Utility methods to calculate message hashes */
public abstract class Hashes {
  private Hashes() {}

  private static final BouncyCastleProvider PROVIDER;

  private static final String KECCAK256 = "KECCAK-256";

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
      // TODO integrate with JCA without performance loose
//      digest = MessageDigest.getInstance(algorithm, "BC");
      digest = new Keccak.Digest256();
      input.update(digest);
      return digest.digest();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Calculates keccak256 hash.
   *
   * @param input input message.
   * @return the hash.
   */
  public static Hash32 keccak256(BytesValue input) {
    byte[] output = digestUsingAlgorithm(input, KECCAK256);
    return Hash32.wrap(Bytes32.wrap(output));
  }
}
