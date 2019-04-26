package org.ethereum.beacon.wire;

import org.ethereum.beacon.wire.message.MessagePayload;
import tech.pegasys.artemis.util.bytes.BytesValue;

public interface PayloadSerializer {

  BytesValue serialize(MessagePayload message);

  MessagePayload deserialize(BytesValue messageBytes);
}
