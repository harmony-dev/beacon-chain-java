package org.ethereum.beacon.discovery.enr;

import org.bouncycastle.math.ec.ECPoint;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.util.Utils;
import org.web3j.crypto.Hash;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes16;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_ID;
import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_IP_V4;
import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_IP_V6;
import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_PKEY_SECP256K1;
import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_TCP_V4;
import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_TCP_V6;
import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_UDP_V4;
import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_UDP_V6;

public class EnrSchemeV4Interpreter implements EnrSchemeInterpreter {

  private Map<String, Function<RlpString, Object>> fieldDecoders = new HashMap<>();

  public EnrSchemeV4Interpreter() {
    fieldDecoders.put(FIELD_PKEY_SECP256K1, rlpString -> BytesValue.wrap(rlpString.getBytes()));
    fieldDecoders.put(
        FIELD_ID, rlpString -> EnrScheme.fromString(new String(rlpString.getBytes())));
    fieldDecoders.put(
        FIELD_IP_V4, rlpString -> Bytes4.wrap(BytesValue.wrap(rlpString.getBytes()), 0));
    fieldDecoders.put(FIELD_TCP_V4, rlpString -> rlpString.asPositiveBigInteger().intValue());
    fieldDecoders.put(FIELD_UDP_V4, rlpString -> rlpString.asPositiveBigInteger().intValue());
    fieldDecoders.put(
        FIELD_IP_V6, rlpString -> Bytes16.wrap(BytesValue.wrap(rlpString.getBytes()), 0));
    fieldDecoders.put(FIELD_TCP_V6, rlpString -> rlpString.asPositiveBigInteger().intValue());
    fieldDecoders.put(FIELD_UDP_V6, rlpString -> rlpString.asPositiveBigInteger().intValue());
  }

  @Override
  public void verify(NodeRecord nodeRecord) {
    EnrSchemeInterpreter.super.verify(nodeRecord);
    if (nodeRecord.get(FIELD_PKEY_SECP256K1) == null) {
      throw new RuntimeException(
          String.format(
              "Field %s not exists but required for scheme %s", FIELD_PKEY_SECP256K1, getScheme()));
    }
    BytesValue pubKey = (BytesValue) nodeRecord.get(FIELD_PKEY_SECP256K1); // compressed
    assert Functions.verifyECDSASignature(
        nodeRecord.getSignature(), nodeRecord.serialize(false), pubKey);
  }

  @Override
  public EnrScheme getScheme() {
    return EnrScheme.V4;
  }

  @Override
  public Bytes32 getNodeId(NodeRecord nodeRecord) {
    verify(nodeRecord);
    BytesValue pkey = (BytesValue) nodeRecord.getKey(FIELD_PKEY_SECP256K1);
    ECPoint pudDestPoint = Functions.publicKeyToPoint(pkey);
    BytesValue xPart =
        Bytes32.wrap(
            Utils.extractBytesFromUnsignedBigInt(pudDestPoint.getXCoord().toBigInteger(), 32));
    BytesValue yPart =
        Bytes32.wrap(
            Utils.extractBytesFromUnsignedBigInt(pudDestPoint.getYCoord().toBigInteger(), 32));
    return Bytes32.wrap(Hash.sha3(xPart.concat(yPart).extractArray()));
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
  public void sign(NodeRecord nodeRecord, Object signOptions) {
    BytesValue privateKey = (BytesValue) signOptions;
    BytesValue signature = Functions.sign(privateKey, nodeRecord.serialize(false));
    nodeRecord.setSignature(signature);
  }

  @Override
  public RlpString encode(String key, Object object) {
    if (object instanceof BytesValue) {
      return fromBytesValue((BytesValue) object);
    } else if (object instanceof Number) {
      return fromNumber((Number) object);
    } else if (object == null) {
      return RlpString.create(new byte[0]);
    } else if (object instanceof EnrScheme) {
      return RlpString.create(((EnrScheme) object).stringName());
    } else {
      throw new RuntimeException(
          String.format(
              "Couldn't serialize node record field %s with value %s: no serializer found.",
              key, object));
    }
  }

  private RlpString fromNumber(Number number) {
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

  private RlpString fromBytesValue(BytesValue bytes) {
    return RlpString.create(bytes.extractArray());
  }
}
