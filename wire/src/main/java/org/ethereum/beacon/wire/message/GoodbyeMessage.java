package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.uint.UInt64;

@SSZSerializable
public class GoodbyeMessage extends MessagePayload {
  public static final UInt64 CLIENT_SHUTDOWN = UInt64.valueOf(1);
  public static final UInt64 IRRELEVANT_NETWORK = UInt64.valueOf(2);
  public static final UInt64 ERROR = UInt64.valueOf(3);

  @SSZ private final UInt64 reason;

  public GoodbyeMessage(UInt64 reason) {
    this.reason = reason;
  }

  public UInt64 getReason() {
    return reason;
  }
}
