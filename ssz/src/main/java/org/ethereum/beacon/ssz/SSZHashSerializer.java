package org.ethereum.beacon.ssz;

import javax.annotation.Nullable;

/** Extends {@link SSZSerializer} to match Hash calculation needs */
public class SSZHashSerializer extends SSZSerializer {

  private static final int HASH_LENGTH = 32;

  public SSZHashSerializer(
      SSZSchemeBuilder schemeBuilder,
      SSZCodecResolver codecResolver,
      SSZModelFactory sszModelFactory) {
    super(schemeBuilder, codecResolver, sszModelFactory);
  }

  /**
   * Shortcut to {@link #encode(Object, Class)}. Resolves class using input object. Not suitable for
   * null values.
   *
   * @param input
   */
  @Override
  public byte[] encode(@Nullable Object input, Class clazz) {
    byte[] preBakedHash = super.encode(input, clazz);
    // For the final output only (ie. not intermediate outputs), if the output is less than 32
    // bytes, right-zero-pad it to 32 bytes.
    byte[] res;
    if (preBakedHash.length < HASH_LENGTH) {
      res = new byte[HASH_LENGTH];
      System.arraycopy(preBakedHash, 0, res, 0, preBakedHash.length);
    } else {
      res = preBakedHash;
    }

    return res;
  }
}
