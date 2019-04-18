package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.message.MessagePayload;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface PayloadSerializer {

  <C extends MessagePayload> BytesValue serialize(C message);

  <C extends MessagePayload> C deserialize(Class<? extends C> messageClass, BytesValue messageBytes);
}
