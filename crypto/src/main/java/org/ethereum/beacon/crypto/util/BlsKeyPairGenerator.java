package org.ethereum.beacon.crypto.util;

import java.math.BigInteger;
import java.util.Random;
import org.bouncycastle.util.BigIntegers;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.bls.bc.BCParameters;
import tech.pegasys.artemis.util.bytes.Bytes32;

/**
 * Given a seed generates a sequence of {@link KeyPair} instances.
 *
 * <p><strong>Note:</strong> the implementation is not secure as it just uses {@link Random}.
 */
public class BlsKeyPairGenerator {
  private static final BigInteger MIN = BigInteger.ONE;
  private static final BigInteger MAX = BCParameters.ORDER.subtract(BigInteger.ONE);

  private Random random;

  private BlsKeyPairGenerator(Random random) {
    this.random = random;
  }

  public static BlsKeyPairGenerator create(long seed) {
    return new BlsKeyPairGenerator(new Random(seed));
  }

  public static BlsKeyPairGenerator createWithoutSeed() {
    return new BlsKeyPairGenerator(new Random());
  }

  public KeyPair next() {
    BigInteger value = BigInteger.ZERO;
    while (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
      byte[] bytes = new byte[Bytes32.SIZE];
      random.nextBytes(bytes);
      value = BigIntegers.fromUnsignedByteArray(bytes);
    }
    PrivateKey privateKey = PrivateKey.create(value);
    return KeyPair.create(privateKey);
  }
}
