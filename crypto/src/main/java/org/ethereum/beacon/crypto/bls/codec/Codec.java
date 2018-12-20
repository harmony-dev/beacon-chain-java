package org.ethereum.beacon.crypto.bls.codec;

import java.util.Arrays;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public interface Codec<P> {

  P decode(BytesValue encoded);

  BytesValue encode(P data);

  int G1_ENCODED_BYTES = 48;
  int G2_ENCODED_BYTES = 2 * G1_ENCODED_BYTES;

  Codec<PointData.G1> G1 =
      new Codec<PointData.G1>() {
        @Override
        public PointData.G1 decode(BytesValue encoded) {
          assert encoded.size() == G1_ENCODED_BYTES;
          byte[] unwrapped = encoded.getArrayUnsafe();

          Flags flags = Flags.read(unwrapped);
          byte[] x = Flags.erase(unwrapped);
          return new PointData.G1(flags, x);
        }

        @Override
        public BytesValue encode(PointData.G1 data) {
          byte[] encoded = data.getX();
          return BytesValue.wrap(data.writeFlags(encoded));
        }
      };

  Codec<PointData.G2> G2 =
      new Codec<PointData.G2>() {
        @Override
        public PointData.G2 decode(BytesValue encoded) {
          assert encoded.size() == G2_ENCODED_BYTES;
          byte[] unwrapped = encoded.getArrayUnsafe();

          Flags flags1 = Flags.read(unwrapped);
          Flags flags2 = Flags.read(unwrapped, G1_ENCODED_BYTES);
          byte[] x1 = Flags.erase(Arrays.copyOf(unwrapped, G1_ENCODED_BYTES));
          byte[] x2 = Arrays.copyOfRange(unwrapped, G1_ENCODED_BYTES, G1_ENCODED_BYTES);

          return new PointData.G2(flags1, flags2, x1, x2);
        }

        @Override
        public BytesValue encode(PointData.G2 data) {
          byte[] encoded = new byte[G2_ENCODED_BYTES];
          System.arraycopy(data.getX1(), 0, encoded, 0, G1_ENCODED_BYTES);
          System.arraycopy(data.getX2(), 0, encoded, G1_ENCODED_BYTES, G1_ENCODED_BYTES);
          return BytesValue.wrap(data.writeFlags(encoded));
        }
      };
}
