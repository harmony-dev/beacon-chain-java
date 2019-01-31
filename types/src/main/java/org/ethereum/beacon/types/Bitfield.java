package org.ethereum.beacon.types;

import tech.pegasys.artemis.util.bytes.ArrayWrappingBytesValue;

public class Bitfield extends ArrayWrappingBytesValue {

  public Bitfield(byte[] bytes) {
    super(bytes);
  }

  public Bitfield and(Bitfield that) {
    int len = Math.max(this.length, that.length);
    byte[] res = new byte[len];
    boolean thisBigger = this.length >= that.length;
    byte[] other;
    if (thisBigger) {
      System.arraycopy(this.getArrayUnsafe(), 0, res, 0, len);
      other = that.getArrayUnsafe();
    } else {
      System.arraycopy(that.getArrayUnsafe(), 0, res, 0, len);
      other = this.getArrayUnsafe();
    }
    for (int i = 0; i < Math.min(this.length, that.length); ++i) {
      res[i] &= other[i];
    }

    return new Bitfield(res);
  }
}
