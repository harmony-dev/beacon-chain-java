package org.ethereum.beacon.wire.message;

import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.MessageSerializer;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SSZMessageSerializer implements MessageSerializer {

  private final SSZSerializer ssz;

  public SSZMessageSerializer(SSZSerializer ssz) {
    this.ssz = ssz;
  }

  @Override
  public BytesValue serialize(Message message) {
    return BytesValue.concat(
        message instanceof RequestMessage ? BytesValue.of(1) : BytesValue.of(2),
        ssz.encode2(message));
  }

  @Override
  public Message deserialize(BytesValue messageBytes) {
    BytesValue body = messageBytes.slice(1);
    return ssz.decode(body, messageBytes.get(0) == 1 ? RequestMessage.class : ResponseMessage.class);
  }
}
