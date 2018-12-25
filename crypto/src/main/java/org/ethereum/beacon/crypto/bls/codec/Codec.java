package org.ethereum.beacon.crypto.bls.codec;

import java.util.Arrays;
import org.ethereum.beacon.crypto.bls.bc.BCParameters;
import org.ethereum.beacon.crypto.bls.milagro.MilagroCodecs;
import tech.pegasys.pantheon.util.bytes.BytesValue;

/**
 * An interface with its implementations to work with representation format of elliptic curve points
 * that is described by bls signature spec. This format is based on compressed representation of the
 * points.
 *
 * <p>Designed to make an abstraction layer between representation format and a certain
 * implementation of elliptic curve math.
 *
 * <p>To stick with specific EC math implementation it assumed to implement yet another codec layer
 * on top of the implementation that this interface contains. See {@link MilagroCodecs}, for
 * example.
 *
 * @param <P> a type that represents decoded point.
 * @see PointData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#point-representations">https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#point-representations</a>
 */
public interface Codec<P> {

  /** Number of bytes occupied by {@code x} point coordinate. */
  int X_SIZE = BCParameters.Q_BYTE_LENGTH;

  /**
   * Decodes point data from a byte sequence. It's assumed that sequence is a point representation
   * format described by the spec.
   *
   * <p><b>Note:</b> this method is not assumed to check validity of encoded data against the
   * format. There is a {@link Validator} interface for this purpose.
   *
   * @param encoded an encoded point data.
   * @return a structure representing decoded point.
   */
  P decode(BytesValue encoded);

  /**
   * Encodes point data.
   *
   * @param point a point.
   * @return a sequence of bytes that represents a point.
   */
  BytesValue encode(P point);

  /**
   * An implementation that works with intermediate format of <code>G<sub>1</sub></code> points.
   *
   * @see PointData.G1
   */
  Codec<PointData.G1> G1 =
      new Codec<PointData.G1>() {
        final int ENCODED_SIZE = X_SIZE;

        @Override
        public PointData.G1 decode(BytesValue encoded) {
          assert encoded.size() == ENCODED_SIZE;
          byte[] x = encoded.extractArray();

          Flags flags = Flags.read(encoded.get(0));
          x[0] = Flags.erase(encoded.get(0));
          return new PointData.G1(flags, x);
        }

        @Override
        public BytesValue encode(PointData.G1 point) {
          assert point.getX().length == ENCODED_SIZE;

          byte[] x = point.getX();
          byte[] encoded = new byte[ENCODED_SIZE];
          if (!point.isInfinity()) {
            System.arraycopy(x, 0, encoded, X_SIZE - x.length, x.length);
          }
          return BytesValue.wrap(point.writeFlags(encoded));
        }
      };

  /**
   * An implementation that works with intermediate format of <code>G<sub>2</sub></code> points.
   *
   * @see PointData.G2
   */
  Codec<PointData.G2> G2 =
      new Codec<PointData.G2>() {
        final int ENCODED_SIZE = 2 * X_SIZE;

        @Override
        public PointData.G2 decode(BytesValue encoded) {
          assert encoded.size() == ENCODED_SIZE;

          Flags flags1 = Flags.read(encoded.get(0));
          Flags flags2 = Flags.read(encoded.get(X_SIZE));

          byte[] x1 = Arrays.copyOf(encoded.getArrayUnsafe(), X_SIZE);
          x1[0] = Flags.erase(encoded.get(0));
          byte[] x2 = Arrays.copyOfRange(encoded.getArrayUnsafe(), X_SIZE, encoded.size());

          return new PointData.G2(flags1, flags2, x1, x2);
        }

        @Override
        public BytesValue encode(PointData.G2 point) {
          assert point.getX1().length == X_SIZE;
          assert point.getX2().length == X_SIZE;

          byte[] x1 = point.getX1();
          byte[] x2 = point.getX2();
          byte[] encoded = new byte[ENCODED_SIZE];
          if (!point.isInfinity()) {
            System.arraycopy(x1, 0, encoded, X_SIZE - x1.length, x1.length);
            System.arraycopy(x2, 0, encoded, 2 * X_SIZE - x2.length, x2.length);
          }

          return BytesValue.wrap(point.writeFlags(encoded));
        }
      };
}
