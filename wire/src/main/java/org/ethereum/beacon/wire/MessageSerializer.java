package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.message.Message;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface MessageSerializer {

  BytesValue serialize(Message message);

  Message deserialize(BytesValue messageBytes);
}
