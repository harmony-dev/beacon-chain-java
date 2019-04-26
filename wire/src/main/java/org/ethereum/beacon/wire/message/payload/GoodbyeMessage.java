package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class GoodbyeMessage extends RequestMessagePayload {
  public static final UInt64 METHOD_ID = UInt64.valueOf(0x1);

  public static final UInt64 CLIENT_SHUTDOWN = UInt64.valueOf(1);
  public static final UInt64 IRRELEVANT_NETWORK = UInt64.valueOf(2);
  public static final UInt64 ERROR = UInt64.valueOf(3);

  @SSZ private final UInt64 reason;

  public GoodbyeMessage(UInt64 reason) {
    this.reason = reason;
  }

  @Override
  public UInt64 getMethodId() {
    return METHOD_ID;
  }

  public UInt64 getReason() {
    return reason;
  }
}
