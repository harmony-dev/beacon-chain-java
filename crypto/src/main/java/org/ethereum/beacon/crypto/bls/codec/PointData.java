package org.ethereum.beacon.crypto.bls.codec;

public interface PointData {

  boolean isInfinity();

  int getSign();

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
  }
}
