package org.ethereum.beacon.wire.impl.plain.channel.beacon;

import java.util.concurrent.atomic.AtomicLong;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import org.ethereum.beacon.wire.impl.plain.channel.RpcChannelMapper;
import org.ethereum.beacon.wire.message.Message;
import org.ethereum.beacon.wire.message.RequestMessage;
import org.ethereum.beacon.wire.message.ResponseMessage;
import org.ethereum.beacon.wire.message.payload.MessageType;
import tech.pegasys.artemis.util.uint.UInt64;

class BeaconRpcMapper extends RpcChannelMapper<Message, RequestMessage, ResponseMessage> {
  private AtomicLong idGen = new AtomicLong(1);

  public BeaconRpcMapper(Channel<Message> inChannel) {
    super(inChannel);
  }

  @Override
  protected boolean isRequest(Message msg) {
    return msg instanceof RequestMessage;
  }

  @Override
  protected boolean isNotification(Message msg) {
    return MessageType.getById(((RequestMessage) msg).getMethodId()).isNotification();
  }

  @Override
  protected Object generateNextId() {
    return UInt64.valueOf(idGen.getAndIncrement());
  }

  @Override
  protected Object getId(Message msg) {
    return msg.getId();
  }

  @Override
  protected void setId(Message msg, Object id) {
    msg.setId((UInt64) id);
  }
}
