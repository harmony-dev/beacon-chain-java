package org.ethereum.beacon.crypto.bls.codec;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.util.Arrays;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public interface PointData {

  boolean isInfinity();

  int getSign();

  BytesValue encode();

  class G1 implements PointData {
    private final Flags flags;
    private final byte[] x;

    G1(Flags flags, byte[] x) {
      this.flags = flags;
      this.x = x;
    }

    public static G1 create(byte[] x, boolean infinity, int sign) {
      return new G1(Flags.create(infinity, sign), x);
    }

    public byte[] getX() {
      return x;
    }

    public Flags getFlags() {
      return flags;
    }

    byte[] writeFlags(byte[] stream) {
      return flags.write(stream);
    }

    @Override
    public boolean isInfinity() {
      return flags.test(Flags.INFINITY) > 0;
    }

    @Override
    public int getSign() {
      return flags.test(Flags.SIGN);
    }

    @Override
    public BytesValue encode() {
      return Codec.G1.encode(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      G1 g1 = (G1) o;
      return Objects.equal(flags, g1.flags) && Arrays.equals(x, g1.x);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("flags", flags)
          .add("x", BytesValue.wrap(x))
          .toString();
    }
  }

  class G2 implements PointData {
    private final Flags flags1;
    private final Flags flags2;
    private final byte[] x1;
    private final byte[] x2;

    G2(Flags flags1, Flags flags2, byte[] x1, byte[] x2) {
      this.flags1 = flags1;
      this.flags2 = flags2;
      this.x1 = x1;
      this.x2 = x2;
    }

    public static G2 create(byte[] x1, byte[] x2, boolean infinity, int sign) {
      return new G2(Flags.create(infinity, sign), Flags.empty(), x1, x2);
    }

    public byte[] getX1() {
      return x1;
    }

    public byte[] getX2() {
      return x2;
    }

    public Flags getFlags1() {
      return flags1;
    }

    public Flags getFlags2() {
      return flags2;
    }

    byte[] writeFlags(byte[] stream) {
      return flags1.write(stream);
    }

    @Override
    public boolean isInfinity() {
      return flags1.test(Flags.INFINITY) > 0;
    }

    @Override
    public int getSign() {
      return flags1.test(Flags.SIGN);
    }

    @Override
    public BytesValue encode() {
      return Codec.G2.encode(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      G2 data = (G2) o;
      return Objects.equal(flags1, data.flags1)
          && Objects.equal(flags2, data.flags2)
          && Arrays.equals(x1, data.x1)
          && Arrays.equals(x2, data.x2);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("flags1", flags1)
          .add("flags2", flags2)
          .add("x1", BytesValue.wrap(x1))
          .add("x2", BytesValue.wrap(x2))
          .toString();
    }
  }
}
