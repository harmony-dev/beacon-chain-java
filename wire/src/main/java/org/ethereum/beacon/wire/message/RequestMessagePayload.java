package org.ethereum.beacon.wire.message;

import tech.pegasys.artemis.util.uint.UInt64;

public abstract class RequestMessagePayload extends MessagePayload {

  public abstract UInt64 getMethodId();
}
