package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.IdentitySchema;
import org.javatuples.Pair;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.util.function.Function;

import static org.web3j.rlp.RlpDecoder.OFFSET_LONG_LIST;
import static org.web3j.rlp.RlpDecoder.OFFSET_SHORT_LIST;

/**
 * Handy utilities used for RLP encoding and decoding and not fulfilled by {@link
 * org.web3j.rlp.RlpEncoder} and {@link RlpDecoder}
 */
public class RlpUtil {
  /**
   * Calculates length of list beginning from the start of the data. So, there could everything else
   * after first list in data, method helps to cut data in this case.
   */
  private static int calcListLen(BytesValue data) {
    int prefix = data.get(0) & 0xFF;
    int prefixAddon = 1;
    if (prefix >= OFFSET_SHORT_LIST && prefix <= OFFSET_LONG_LIST) {

      // 4. the data is a list if the range of the
      // first byte is [0xc0, 0xf7], and the concatenation of
      // the RLP encodings of all items of the list which the
      // total payload is equal to the first byte minus 0xc0 follows the first byte;

      byte listLen = (byte) (prefix - OFFSET_SHORT_LIST);
      return listLen & 0xFF + prefixAddon;
    } else if (prefix > OFFSET_LONG_LIST) {

      // 5. the data is a list if the range of the
      // first byte is [0xf8, 0xff], and the total payload of the
      // list which length is equal to the
      // first byte minus 0xf7 follows the first byte,
      // and the concatenation of the RLP encodings of all items of
      // the list follows the total payload of the list;

      int lenOfListLen = (prefix - OFFSET_LONG_LIST) & 0xFF;
      prefixAddon += lenOfListLen;
      return UInt64.fromBytesBigEndian(Bytes8.leftPad(data.slice(1, lenOfListLen & 0xFF)))
              .intValue()
          + prefixAddon;
    } else {
      throw new RuntimeException("Not a start of RLP list!!");
    }
  }

  /**
   * @return first rlp list in provided data, plus remaining data starting from the end of this list
   */
  public static Pair<RlpList, BytesValue> decodeFirstList(BytesValue data) {
    int len = RlpUtil.calcListLen(data);
    return Pair.with(RlpDecoder.decode(data.slice(0, len).extractArray()), data.slice(len));
  }

  /**
   * Encodes object to {@link RlpString}. Supports numbers, {@link BytesValue} etc.
   *
   * @throws RuntimeException with errorMessageFunction applied with `object` when encoding is not
   *     possible
   */
  public static RlpString encode(Object object, Function<Object, String> errorMessageFunction) {
    if (object instanceof BytesValue) {
      return fromBytesValue((BytesValue) object);
    } else if (object instanceof Number) {
      return fromNumber((Number) object);
    } else if (object == null) {
      return RlpString.create(new byte[0]);
    } else if (object instanceof IdentitySchema) {
      return RlpString.create(((IdentitySchema) object).stringName());
    } else {
      throw new RuntimeException(errorMessageFunction.apply(object));
    }
  }

  private static RlpString fromNumber(Number number) {
    if (number instanceof BigInteger) {
      return RlpString.create((BigInteger) number);
    } else if (number instanceof Long) {
      return RlpString.create((Long) number);
    } else if (number instanceof Integer) {
      return RlpString.create((Integer) number);
    } else {
      throw new RuntimeException(
          String.format("Couldn't serialize number %s : no serializer found.", number));
    }
  }

  private static RlpString fromBytesValue(BytesValue bytes) {
    return RlpString.create(bytes.extractArray());
  }
}
