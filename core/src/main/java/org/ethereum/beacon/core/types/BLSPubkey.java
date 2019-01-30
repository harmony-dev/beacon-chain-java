package org.ethereum.beacon.core.types;

import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.DelegatingBytes48;

public class BLSPubkey extends DelegatingBytes48 {

  public static final BLSPubkey ZERO = new BLSPubkey(Bytes48.ZERO);

  private BLSPubkey(final Bytes48 bytes) {
    super(bytes);
  }

  public static BLSPubkey wrap(final Bytes48 bytes) {
    return new BLSPubkey(bytes);
  }

  /**
   * Parse an hexadecimal string representing a hash value.
   *
   * @param str An hexadecimal string (with or without the leading '0x') representing a valid hash
   *     value.
   * @return The parsed hash.
   * @throws NullPointerException if the provided string is {@code null}.
   * @throws IllegalArgumentException if the string is either not hexadecimal, or not the valid
   *     representation of a hash (not 32 bytes).
   */
  public static BLSPubkey fromHexString(final String str) {
    return new BLSPubkey(Bytes48.fromHexStringStrict(str));
  }

  public static BLSPubkey fromHexStringLenient(final String str) {
    return new BLSPubkey(Bytes48.fromHexStringLenient(str));
  }
}
