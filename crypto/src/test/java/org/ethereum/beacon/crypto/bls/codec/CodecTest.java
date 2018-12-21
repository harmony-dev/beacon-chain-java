package org.ethereum.beacon.crypto.bls.codec;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Random;
import org.bouncycastle.util.Arrays;
import org.ethereum.beacon.crypto.bls.bc.BCParameters;
import org.junit.Test;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public class CodecTest {

  @Test
  public void readWriteFlags() {
    byte[] stream = new byte[1];
    Flags origin = Flags.create(false, 1);

    byte[] flagged = origin.write(stream);
    assertThat(flagged[0]).isEqualTo((byte) 0xA0);

    Flags read = Flags.read(flagged);
    assertThat(read).isEqualTo(origin);

    byte[] cleaned = Flags.erase(flagged);
    assertThat(cleaned[0]).isEqualTo((byte) 0x00);
  }

  @Test
  public void encodeInfinityInG1() {
    byte[] x = randomX();

    BytesValue encoded = Codec.G1.encode(PointData.G1.create(x, true, 1));
    Flags flags = Flags.read(encoded.getArrayUnsafe());

    assertThat(flags.test(Flags.C)).isEqualTo(1);
    assertThat(flags.test(Flags.INFINITY)).isEqualTo(1);
    assertThat(flags.test(Flags.SIGN)).isEqualTo(0);

    byte[] withoutFlags = Flags.erase(encoded.getArrayUnsafe());
    assertThat(withoutFlags).isEqualTo(new byte[BCParameters.Q_BYTE_LENGTH]);

    PointData.G1 decoded = Codec.G1.decode(encoded);
    assertThat(decoded.getFlags()).isEqualTo(flags);
    assertThat(decoded.getX()).isEqualTo(new byte[BCParameters.Q_BYTE_LENGTH]);
  }

  @Test
  public void encodeG1WithSign() {
    byte[] x = randomX();

    BytesValue encoded = Codec.G1.encode(PointData.G1.create(x, false, 1));
    Flags flags = Flags.read(encoded.getArrayUnsafe());

    assertThat(flags.test(Flags.C)).isEqualTo(1);
    assertThat(flags.test(Flags.INFINITY)).isEqualTo(0);
    assertThat(flags.test(Flags.SIGN)).isEqualTo(1);

    byte[] withoutFlags = Flags.erase(encoded.getArrayUnsafe());
    assertThat(withoutFlags).isEqualTo(x);

    PointData.G1 decoded = Codec.G1.decode(encoded);
    assertThat(decoded.getFlags()).isEqualTo(flags);
    assertThat(decoded.getX()).isEqualTo(x);
  }

  @Test
  public void encodeInfinityInG2() {
    byte[] x1 = randomX();
    byte[] x2 = randomX();

    BytesValue encoded = Codec.G2.encode(PointData.G2.create(x1, x2, true, 1));
    Flags flags = Flags.read(encoded.getArrayUnsafe());

    assertThat(flags.test(Flags.C)).isEqualTo(1);
    assertThat(flags.test(Flags.INFINITY)).isEqualTo(1);
    assertThat(flags.test(Flags.SIGN)).isEqualTo(0);

    byte[] withoutFlags = Flags.erase(encoded.getArrayUnsafe());
    assertThat(withoutFlags).isEqualTo(new byte[BCParameters.Q_BYTE_LENGTH * 2]);

    PointData.G2 decoded = Codec.G2.decode(encoded);
    assertThat(decoded.getFlags1()).isEqualTo(flags);
    assertThat(decoded.getFlags2().isZero()).isTrue();
    assertThat(decoded.getX1()).isEqualTo(new byte[BCParameters.Q_BYTE_LENGTH]);
    assertThat(decoded.getX2()).isEqualTo(new byte[BCParameters.Q_BYTE_LENGTH]);
  }

  @Test
  public void encodeG2WithSign() {
    byte[] x1 = randomX();
    byte[] x2 = randomX();

    BytesValue encoded = Codec.G2.encode(PointData.G2.create(x1, x2, false, 1));
    Flags flags = Flags.read(encoded.getArrayUnsafe());

    assertThat(flags.test(Flags.C)).isEqualTo(1);
    assertThat(flags.test(Flags.INFINITY)).isEqualTo(0);
    assertThat(flags.test(Flags.SIGN)).isEqualTo(1);

    byte[] withoutFlags = Flags.erase(encoded.getArrayUnsafe());
    assertThat(withoutFlags).isEqualTo(Arrays.concatenate(x1, x2));

    PointData.G2 decoded = Codec.G2.decode(encoded);
    assertThat(decoded.getFlags1()).isEqualTo(flags);
    assertThat(decoded.getFlags2().isZero()).isTrue();
    assertThat(decoded.getX1()).isEqualTo(x1);
    assertThat(decoded.getX2()).isEqualTo(x2);
  }

  @Test
  public void checkG1WithRandomData() {
    PointData.G1 data = randomDataG1();
    BytesValue encoded = Codec.G1.encode(data);
    PointData.G1 decoded = Codec.G1.decode(encoded);

    assertThat(decoded).isEqualTo(data);
  }

  @Test
  public void checkG2WithRandomData() {
    PointData.G2 data = randomDataG2();
    BytesValue encoded = Codec.G2.encode(data);
    PointData.G2 decoded = Codec.G2.decode(encoded);

    assertThat(decoded).isEqualTo(data);
  }

  PointData.G1 randomDataG1() {
    Random random = new Random();
    byte[] x = new byte[BCParameters.Q_BYTE_LENGTH];
    boolean infinity = random.nextBoolean();
    int sign = random.nextBoolean() ? 1 : 0;

    if (!infinity) {
      random.nextBytes(x);
      x = Flags.erase(x);
    }

    return PointData.G1.create(x, infinity, sign);
  }

  PointData.G2 randomDataG2() {
    Random random = new Random();
    byte[] x1 = new byte[BCParameters.Q_BYTE_LENGTH];
    byte[] x2 = new byte[BCParameters.Q_BYTE_LENGTH];
    boolean infinity = random.nextBoolean();
    int sign = random.nextBoolean() ? 1 : 0;

    if (!infinity) {
      random.nextBytes(x1);
      x1 = Flags.erase(x1);

      random.nextBytes(x2);
      x2 = Flags.erase(x2);
    }

    return PointData.G2.create(x1, x2, infinity, sign);
  }

  byte[] randomX() {
    return new BigInteger(381, new Random()).mod(BCParameters.Q).toByteArray();
  }
}
