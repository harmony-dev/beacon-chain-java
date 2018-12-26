package tech.pegasys.pantheon.util.bytes;

import static com.google.common.base.Preconditions.checkArgument;

/** An implementation of {@link Bytes8} backed by a byte array ({@code byte[]}). */
class ArrayWrappingBytes8 extends ArrayWrappingBytesValue implements Bytes8 {

  ArrayWrappingBytes8(final byte[] bytes) {
    this(checkLength(bytes), 0);
  }

  ArrayWrappingBytes8(final byte[] bytes, final int offset) {
    super(checkLength(bytes, offset), offset, SIZE);
  }

  // Ensures a proper error message.
  private static byte[] checkLength(final byte[] bytes) {
    checkArgument(bytes.length == SIZE, "Expected %s bytes but got %s", SIZE, bytes.length);
    return bytes;
  }

  // Ensures a proper error message.
  private static byte[] checkLength(final byte[] bytes, final int offset) {
    checkArgument(
        bytes.length - offset >= SIZE,
        "Expected at least %s bytes from offset %s but got only %s",
        SIZE,
        offset,
        bytes.length - offset);
    return bytes;
  }

  @Override
  public Bytes8 copy() {
    // Because MutableArrayWrappingBytesValue overrides this, we know we are immutable. We may
    // retain more than necessary however.
    if (offset == 0 && length == bytes.length) return this;

    return new ArrayWrappingBytes8(arrayCopy());
  }
}
