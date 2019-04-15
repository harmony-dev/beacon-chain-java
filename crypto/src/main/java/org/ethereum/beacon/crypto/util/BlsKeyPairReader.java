package org.ethereum.beacon.crypto.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;

/**
 * Given a file or resource reads BLS {@link KeyPair} instances from it.
 *
 * <p>Expects an input as a sequence of lines formatted as {@link KeyPair#asString()}.
 *
 * <p>Supports inputs in GZIP format, requires ".gz" suffix.
 */
public class BlsKeyPairReader {

  private final BufferedReader reader;
  private int pairsRead = 0;

  public BlsKeyPairReader(BufferedReader reader) {
    this.reader = reader;
  }

  /**
   * Instantiates reader with predefined key pair source located in {@code /bls-key-pairs/} resource
   * folder.
   *
   * @return an instance of key pair reader.
   */
  public static BlsKeyPairReader createWithDefaultSource() {
    return createForResource("/bls-key-pairs/bls-pairs-1m-seed_1.gz");
  }

  public static BlsKeyPairReader createForResource(String resourceName) {
    try {
      return createFromStream(ClassLoader.class.getResourceAsStream(resourceName), resourceName);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public static BlsKeyPairReader createForFile(String name) {
    try {
      return createFromStream(new FileInputStream(new File(name)), name);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public static BlsKeyPairReader createFromStream(InputStream input, String name)
      throws IOException {
    BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(name.endsWith(".gz") ? new GZIPInputStream(input) : input));
    return new BlsKeyPairReader(reader);
  }

  /**
   * Fetches next pair from the source.
   *
   * @return a key pair.
   * @throws RuntimeException if any error occurred during interaction with underlying source.
   */
  public KeyPair next() {
    try {
      String parts = reader.readLine();
      if (parts == null) {
        throw new RuntimeException("EOF, key pairs read: " + pairsRead);
      }
      int delimiterPos = parts.indexOf(":");
      if (delimiterPos < 0) {
        throw new RuntimeException("Invalid input: " + parts);
      }
      PrivateKey privateKey =
          PrivateKey.create(Bytes32.fromHexString(parts.substring(0, delimiterPos)));
      PublicKey publicKey =
          PublicKey.createWithoutValidation(
              Bytes48.fromHexString(parts.substring(delimiterPos + 1)));

      pairsRead += 1;
      return new KeyPair(publicKey, privateKey);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
