package org.ethereum.beacon.discovery.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.List;

/** UDP Packet -> BytesValue converter with default Netty interface */
public class DatagramToBytesValue extends MessageToMessageDecoder<DatagramPacket> {
  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out)
      throws Exception {
    ByteBuf buf = msg.content();
    byte[] data = new byte[buf.readableBytes()];
    buf.readBytes(data);
    out.add(BytesValue.wrap(data));
  }
}
