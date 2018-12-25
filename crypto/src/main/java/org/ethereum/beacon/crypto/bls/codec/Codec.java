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
 * <p>To stick with specific EC math implementation it assumed to build yet another codec layer on
 * top of this one. See {@link MilagroCodecs}, for example.
 *
 * @param <P> an implementation of type that holds decoded point elements.
 * @see PointData
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#point-representations">https://github.com/ethereum/eth2.0-specs/blob/master/specs/bls_signature.md#point-representations</a>
 */
public interface Codec<P> {

  /** Number of bytes occupied by {@code x} point coordinate. */
  int X_SIZE = BCParameters.Q_BYTE_LENGTH;

  /**
   * Decodes a byte sequence representing
   *
   * @param encoded
   * @return
   */
  P decode(BytesValue encoded);

  BytesValue encode(P data);

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
        public BytesValue encode(PointData.G1 data) {
          assert data.getX().length == ENCODED_SIZE;

          byte[] x = data.getX();
          byte[] encoded = new byte[ENCODED_SIZE];
          if (!data.isInfinity()) {
            System.arraycopy(x, 0, encoded, X_SIZE - x.length, x.length);
          }
          return BytesValue.wrap(data.writeFlags(encoded));
        }
      };

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
        public BytesValue encode(PointData.G2 data) {
          assert data.getX1().length == X_SIZE;
          assert data.getX2().length == X_SIZE;

          byte[] x1 = data.getX1();
          byte[] x2 = data.getX2();
          byte[] encoded = new byte[ENCODED_SIZE];
          if (!data.isInfinity()) {
            System.arraycopy(x1, 0, encoded, X_SIZE - x1.length, x1.length);
            System.arraycopy(x2, 0, encoded, 2 * X_SIZE - x2.length, x2.length);
          }

          return BytesValue.wrap(data.writeFlags(encoded));
        }
      };
}
