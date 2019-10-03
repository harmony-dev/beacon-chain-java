package org.ethereum.beacon.wire.impl.libp2p.encoding;

import io.netty.buffer.ByteBuf;

public interface MessageCodec<TMessage> {

  void serialize(TMessage msg, ByteBuf buf);

  TMessage deserialize(ByteBuf buf);
}

