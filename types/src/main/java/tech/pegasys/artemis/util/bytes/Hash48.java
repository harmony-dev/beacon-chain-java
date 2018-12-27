package tech.pegasys.artemis.util.bytes;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Hash48 extends DelegatingBytes48 {

  public static final Hash48 ZERO = new Hash48(Bytes48.ZERO);

  private Hash48(final Bytes48 bytes) {
    super(bytes);
  }

  public static Hash48 wrap(final Bytes48 bytes) {
    return new Hash48(bytes);
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
  @JsonCreator
  public static Hash48 fromHexString(final String str) {
    return new Hash48(Bytes48.fromHexStringStrict(str));
  }

  public static Hash48 fromHexStringLenient(final String str) {
    return new Hash48(Bytes48.fromHexStringLenient(str));
  }
}
