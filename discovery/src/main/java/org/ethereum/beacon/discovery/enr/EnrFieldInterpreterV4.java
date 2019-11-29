package org.ethereum.beacon.discovery.enr;

import org.ethereum.beacon.discovery.RlpUtil;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.Bytes16;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EnrFieldInterpreterV4 implements EnrFieldInterpreter {
  public static EnrFieldInterpreterV4 DEFAULT = new EnrFieldInterpreterV4();

  private Map<String, Function<RlpString, Object>> fieldDecoders = new HashMap<>();

  public EnrFieldInterpreterV4() {
    fieldDecoders.put(
        EnrFieldV4.PKEY_SECP256K1, rlpString -> BytesValue.wrap(rlpString.getBytes()));
    fieldDecoders.put(
        EnrField.ID, rlpString -> IdentitySchema.fromString(new String(rlpString.getBytes())));
    fieldDecoders.put(
        EnrField.IP_V4, rlpString -> Bytes4.wrap(BytesValue.wrap(rlpString.getBytes()), 0));
    fieldDecoders.put(EnrField.TCP_V4, rlpString -> rlpString.asPositiveBigInteger().intValue());
    fieldDecoders.put(EnrField.UDP_V4, rlpString -> rlpString.asPositiveBigInteger().intValue());
    fieldDecoders.put(
        EnrField.IP_V6, rlpString -> Bytes16.wrap(BytesValue.wrap(rlpString.getBytes()), 0));
    fieldDecoders.put(EnrField.TCP_V6, rlpString -> rlpString.asPositiveBigInteger().intValue());
    fieldDecoders.put(EnrField.UDP_V6, rlpString -> rlpString.asPositiveBigInteger().intValue());
  }

  @Override
  public Object decode(String key, RlpString rlpString) {
    Function<RlpString, Object> fieldDecoder = fieldDecoders.get(key);
    if (fieldDecoder == null) {
      throw new RuntimeException(String.format("No decoder found for field `%s`", key));
    }
    return fieldDecoder.apply(rlpString);
  }

  @Override
  public RlpType encode(String key, Object object) {
    return RlpUtil.encode(
        object,
        o ->
            String.format(
                "Couldn't encode field %s with value %s: no serializer found.", key, object));
  }
}
