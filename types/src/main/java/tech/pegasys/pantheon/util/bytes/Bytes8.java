/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.util.bytes;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A {@link BytesValue} that is guaranteed to contain exactly 8 bytes.
 */
public interface Bytes8 extends BytesValue {
  int SIZE = 8;

  Bytes8 ZERO = wrap(new byte[8]);

  /**
   * Wraps the provided byte array, which must be of length 8, as a {@link Bytes8}.
   *
   * <p>Note that value is not copied, only wrapped, and thus any future update to {@code value}
   * will be reflected in the returned value.
   *
   * @param bytes The bytes to wrap.
   * @return A {@link Bytes8} wrapping {@code value}.
   * @throws IllegalArgumentException if {@code value.length != 8}.
   */
  static Bytes8 wrap(final byte[] bytes) {
    checkArgument(bytes.length == SIZE, "Expected %s bytes but got %s", SIZE, bytes.length);
    return wrap(bytes, 0);
  }

  /**
   * Wraps a slice/sub-part of the provided array as a {@link Bytes8}.
   *
   * <p>Note that value is not copied, only wrapped, and thus any future update to {@code value}
   * within the wrapped parts will be reflected in the returned value.
   *
   * @param bytes The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned
   *     value. In other words, you will have {@code wrap(value, i).get(0) == value[i]}.
   * @return A {@link Bytes8} that exposes the bytes of {@code value} from {@code offset}
   *     (inclusive) to {@code offset + 8} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset &lt; 0 || (value.length &gt; 0 && offset >=
   *     value.length)}.
   * @throws IllegalArgumentException if {@code length &lt; 0 || offset + 8 &gt; value.length}.
   */
  static Bytes8 wrap(final byte[] bytes, final int offset) {
    return new ArrayWrappingBytes8(bytes, offset);
  }

  /**
   * Wraps a slice/sub-part of the provided value as a {@link Bytes8}.
   *
   * <p>Note that value is not copied, only wrapped, and thus any future update to {@code value}
   * within the wrapped parts will be reflected in the returned value.
   *
   * @param bytes The bytes to wrap.
   * @param offset The index (inclusive) in {@code value} of the first byte exposed by the returned
   *     value. In other words, you will have {@code wrap(value, i).get(0) == value.get(i)}.
   * @return A {@link Bytes8} that exposes the bytes of {@code value} from {@code offset}
   *     (inclusive) to {@code offset + 8} (exclusive).
   * @throws IndexOutOfBoundsException if {@code offset &lt; 0 || (value.size() &gt; 0 && offset >=
   *     value.size())}.
   * @throws IllegalArgumentException if {@code length &lt; 0 || offset + 8 &gt; value.size()}.
   */
  static Bytes8 wrap(final BytesValue bytes, final int offset) {
    final BytesValue slice = bytes.slice(offset, Bytes8.SIZE);
    return slice instanceof Bytes8 ? (Bytes8) slice : new ArrayWrappingBytes8(slice.extractArray());
  }

  /**
   * Parse an hexadecimal string into a {@link Bytes8}.
   *
   * <p>This method is lenient in that {@code str} may of an odd length, in which case it will
   * behave exactly as if it had an additional 0 in front.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That
   *     representation may contain less than 8 bytes, in which case the result is left padded with
   *     zeros (see {@link #fromHexStringStrict} if this is not what you want).
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal
   *     representation or contains more than 8 bytes.
   */
  static Bytes8 fromHexStringLenient(final String str) {
    return wrap(BytesValues.fromRawHexString(str, SIZE, true));
  }

  /**
   * Parse an hexadecimal string into a {@link Bytes8}.
   *
   * <p>This method is strict in that {@code str} must of an even length.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That
   *     representation may contain less than 8 bytes, in which case the result is left padded with
   *     zeros (see {@link #fromHexStringStrict} if this is not what you want).
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal
   *     representation, is of an odd length, or contains more than 8 bytes.
   */
  static Bytes8 fromHexString(final String str) {
    return wrap(BytesValues.fromRawHexString(str, SIZE, false));
  }

  /**
   * Parse an hexadecimal string into a {@link Bytes8}.
   *
   * <p>This method is extra strict in that {@code str} must of an even length and the provided
   * representation must have exactly 8 bytes.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to valid hexadecimal
   *     representation, is of an odd length or does not contain exactly 8 bytes.
   */
  static Bytes8 fromHexStringStrict(final String str) {
    return wrap(BytesValues.fromRawHexString(str, -1, false));
  }

  @Override
  default int size() {
    return SIZE;
  }

  @Override
  Bytes8 copy();
}
