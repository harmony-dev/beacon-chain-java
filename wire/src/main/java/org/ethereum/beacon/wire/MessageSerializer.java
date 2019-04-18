package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.message.Message;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface MessageSerializer {

  <C extends Message> BytesValue serialize(C message);

  <C extends Message> C deserialize(Class<? extends C> messageClass, BytesValue messageBytes);
}
