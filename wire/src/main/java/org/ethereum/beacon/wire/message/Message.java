package org.ethereum.beacon.wire.message;

import tech.pegasys.artemis.util.bytes.BytesValue;

public abstract class Message {

  public abstract MessagePayload getPayload();
}
