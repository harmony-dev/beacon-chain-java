package org.ethereum.beacon.discovery.enr;

import org.bouncycastle.math.ec.ECPoint;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.util.Utils;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class IdentitySchemaV4Interpreter implements IdentitySchemaInterpreter {
  @Override
  public void verify(NodeRecord nodeRecord) {
    IdentitySchemaInterpreter.super.verify(nodeRecord);
    if (nodeRecord.get(EnrFieldV4.PKEY_SECP256K1) == null) {
      throw new RuntimeException(
          String.format(
              "Field %s not exists but required for scheme %s",
              EnrFieldV4.PKEY_SECP256K1, getScheme()));
    }
    BytesValue pubKey = (BytesValue) nodeRecord.get(EnrFieldV4.PKEY_SECP256K1); // compressed
    assert Functions.verifyECDSASignature(
        nodeRecord.getSignature(), Functions.hashKeccak(nodeRecord.serializeNoSignature()), pubKey);
  }

  @Override
  public IdentitySchema getScheme() {
    return IdentitySchema.V4;
  }

  @Override
  public Bytes32 getNodeId(NodeRecord nodeRecord) {
    verify(nodeRecord);
    BytesValue pkey = (BytesValue) nodeRecord.getKey(EnrFieldV4.PKEY_SECP256K1);
    ECPoint pudDestPoint = Functions.publicKeyToPoint(pkey);
    BytesValue xPart =
        Bytes32.wrap(
            Utils.extractBytesFromUnsignedBigInt(pudDestPoint.getXCoord().toBigInteger(), 32));
    BytesValue yPart =
        Bytes32.wrap(
            Utils.extractBytesFromUnsignedBigInt(pudDestPoint.getYCoord().toBigInteger(), 32));
    return Functions.hashKeccak(xPart.concat(yPart));
  }

  @Override
  public void sign(NodeRecord nodeRecord, Object signOptions) {
    BytesValue privateKey = (BytesValue) signOptions;
    BytesValue signature =
        Functions.sign(privateKey, Functions.hashKeccak(nodeRecord.serializeNoSignature()));
    nodeRecord.setSignature(signature);
  }
}
