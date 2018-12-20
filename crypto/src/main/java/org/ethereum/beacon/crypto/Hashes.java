package org.ethereum.beacon.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public abstract class Hashes {
  private Hashes() {}

  private static final BouncyCastleProvider PROVIDER;

  private static final String KECCAK256 = "KECCAK-256";
  private static final String KECCAK512 = "KECCAK-512";

  static {
    Security.addProvider(PROVIDER = new BouncyCastleProvider());
  }

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

  public static Bytes32 keccack256(BytesValue input) {
    byte[] output = digestUsingAlgorithm(input, KECCAK256);
    return Bytes32.wrap(output);
  }

  public static BytesValue keccack384(BytesValue input) {
    byte[] output = digestUsingAlgorithm(input, KECCAK512);
    return BytesValue.wrap(output, 0, 48);
  }
}
