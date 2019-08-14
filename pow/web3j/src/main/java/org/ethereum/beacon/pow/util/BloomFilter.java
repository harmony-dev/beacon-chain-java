package org.ethereum.beacon.pow.util;

import java.util.Arrays;

/**
 * Bloom Filter function for ETH1
 */
public class BloomFilter {
  private final static int _8_STEPS = 8;
  private final static int _3_LOW_BITS = 7;
  private final static int ENSURE_BYTE = 255;

  private byte[] bytes;

  public BloomFilter(byte[] bytes) {
    this.bytes = bytes;
  }

  /** Creates bloom filter for provided topic. For address use address hash. */
  public static BloomFilter create(byte[] topic) {
    int mov1 = (((topic[0] & ENSURE_BYTE) & (_3_LOW_BITS)) << _8_STEPS) + ((topic[1]) & ENSURE_BYTE);
    int mov2 = (((topic[2] & ENSURE_BYTE) & (_3_LOW_BITS)) << _8_STEPS) + ((topic[3]) & ENSURE_BYTE);
    int mov3 = (((topic[4] & ENSURE_BYTE) & (_3_LOW_BITS)) << _8_STEPS) + ((topic[5]) & ENSURE_BYTE);

    byte[] data = new byte[256];
    BloomFilter bloom = new BloomFilter(data);
    setBit(data, mov1);
    setBit(data, mov2);
    setBit(data, mov3);

    return bloom;
  }

  /**
   * Sets bit to "true" (1)
   */
  private static void setBit(byte[] data, int pos) {
    int posByte = data.length - 1 - (pos / 8);
    int posBit = pos % 8;
    byte oldByte = data[posByte];
    byte newByte = (byte) (oldByte | 1 << posBit);

    data[posByte] = newByte;
  }

  public BloomFilter or(BloomFilter bloom) {
    byte[] copy = new byte[bytes.length];
    System.arraycopy(bytes, 0, copy, 0, bytes.length);
    for (int i = 0; i < bytes.length; ++i) {
      copy[i] |= bloom.bytes[i];
    }

    return new BloomFilter(copy);
  }

  /** Checks whether this filter matches another = includes event from another filter */
  public boolean matches(BloomFilter topicBloom) {
    return this.equals(this.or(topicBloom));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BloomFilter that = (BloomFilter) o;

    return Arrays.equals(bytes, that.bytes);
  }
}
