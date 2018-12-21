package org.ethereum.beacon.crypto.bls.codec;

import java.util.Arrays;
import org.ethereum.beacon.crypto.bls.bc.BCParameters;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public interface Codec<P> {

  int X_SIZE = BCParameters.Q_BYTE_LENGTH;

  P decode(BytesValue encoded);

  BytesValue encode(P data);

  Codec<PointData.G1> G1 =
      new Codec<PointData.G1>() {
        final int ENCODED_SIZE = X_SIZE;

        @Override
        public PointData.G1 decode(BytesValue encoded) {
          assert encoded.size() == ENCODED_SIZE;
          byte[] unwrapped = encoded.getArrayUnsafe();

          Flags flags = Flags.read(unwrapped);
          byte[] x = Flags.erase(unwrapped);
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
          byte[] unwrapped = encoded.getArrayUnsafe();

          Flags flags1 = Flags.read(unwrapped);
          Flags flags2 = Flags.read(unwrapped, X_SIZE);
          byte[] x1 = Flags.erase(Arrays.copyOf(unwrapped, X_SIZE));
          byte[] x2 = Arrays.copyOfRange(unwrapped, X_SIZE, unwrapped.length);

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
