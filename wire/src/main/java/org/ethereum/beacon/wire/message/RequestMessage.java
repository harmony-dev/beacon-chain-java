package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class RequestMessage extends Message {
  @SSZ
  private UInt64 id;
  @SSZ(type = "uint16")
  private final int methodId;
  @SSZ
  private final BytesValue body;

  public RequestMessage(int methodId, BytesValue body) {
    this.methodId = methodId;
    this.body = body;
  }

  public RequestMessage(UInt64 id, int methodId, BytesValue body) {
    this.id = id;
    this.methodId = methodId;
    this.body = body;
  }

  public UInt64 getId() {
    return id;
  }

  public void setId(UInt64 id) {
    this.id = id;
  }

  public int getMethodId() {
    return methodId;
  }

  public BytesValue getBody() {
    return body;
  }

  @Override
  public RequestMessagePayload getPayload() {
    return null;
  }
}
