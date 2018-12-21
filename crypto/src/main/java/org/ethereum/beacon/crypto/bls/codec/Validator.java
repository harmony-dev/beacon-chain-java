package org.ethereum.beacon.crypto.bls.codec;

import java.math.BigInteger;
import org.ethereum.beacon.crypto.bls.bc.BCParameters;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public interface Validator {

  Result validate(BytesValue encoded);

  Validator G1 =
      new Validator() {
        static final int ENCODED_SIZE = 48;

        @Override
        public Result validate(BytesValue encoded) {
          if (encoded.size() != ENCODED_SIZE) {
            return Result.invalid(
                String.format(
                    "unexpected length of encoded G1, should be %d got %d",
                    ENCODED_SIZE, encoded.size()));
          }

          PointData.G1 data = Codec.G1.decode(encoded);

          int aFlag = data.getFlags().test(Flags.A);
          int bFlag = data.getFlags().test(Flags.B);
          int cFlag = data.getFlags().test(Flags.C);
          BigInteger x = new BigInteger(1, data.getX());

          if (cFlag != 1) {
            return Result.invalid("invalid c_flag, should always be 1 but got " + cFlag);
          }

          if (bFlag == 1) {
            if (aFlag != 0) {
              return Result.invalid(
                  "invalid a_flag, should be 0 when b_flag == 1 but got " + aFlag);
            }
            if (x.compareTo(BigInteger.ZERO) != 0) {
              return Result.invalid("invalid x, should be 0 when b_flag == 1 but got " + x);
            }
          }

          if (BCParameters.Q.compareTo(x) <= 0) {
            return Result.invalid("invalid x, should be < Q");
          }

          return Result.Valid;
        }
      };

  Validator G2 =
      new Validator() {
        static final int ENCODED_SIZE = 96;

        @Override
        public Result validate(BytesValue encoded) {
          if (encoded.size() != ENCODED_SIZE) {
            return Result.invalid(
                String.format(
                    "unexpected length of encoded G2, should be %d got %d",
                    ENCODED_SIZE, encoded.size()));
          }

          PointData.G2 data = Codec.G2.decode(encoded);

          int aFlag = data.getFlags1().test(Flags.A);
          int bFlag = data.getFlags1().test(Flags.B);
          int cFlag = data.getFlags1().test(Flags.C);
          Flags flags2 = data.getFlags2();
          BigInteger x1 = new BigInteger(1, data.getX1());
          BigInteger x2 = new BigInteger(1, data.getX2());

          if (!flags2.isZero()) {
            return Result.invalid("invalid flag2 value, should always be 0");
          }

          if (cFlag != 1) {
            return Result.invalid("invalid c_flag, should always be 1 but got " + cFlag);
          }

          if (bFlag == 1) {
            if (aFlag != 0) {
              return Result.invalid(
                  "invalid a_flag, should be 0 when b_flag == 1 but got " + aFlag);
            }
            if (x1.compareTo(BigInteger.ZERO) != 0) {
              return Result.invalid("invalid x1, should be 0 when b_flag == 1 but got " + x1);
            }
            if (x2.compareTo(BigInteger.ZERO) != 0) {
              return Result.invalid("invalid x2, should be 0 when b_flag == 1 but got " + x2);
            }
          }

          if (BCParameters.Q.compareTo(x1) <= 0) {
            return Result.invalid("invalid x1, should be < Q");
          }
          if (BCParameters.Q.compareTo(x2) <= 0) {
            return Result.invalid("invalid x2, should be < Q");
          }

          return Result.Valid;
        }
      };

  class Result {
    private static final Result Valid = new Result(true, "");

    final boolean valid;
    final String message;

    Result(boolean valid, String message) {
      this.valid = valid;
      this.message = message;
    }

    static Result invalid(String message) {
      return new Result(false, message);
    }

    public boolean isValid() {
      return valid;
    }

    public String getMessage() {
      return message;
    }
  }
}
