package org.ethereum.beacon.wire.message;

import tech.pegasys.artemis.util.uint.UInt64;

public abstract class Message {

  public abstract MessagePayload getPayload();

  public abstract UInt64 getId();

  public abstract void setId(UInt64 id);

}
