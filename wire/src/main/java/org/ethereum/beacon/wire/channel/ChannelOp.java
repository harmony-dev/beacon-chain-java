package org.ethereum.beacon.wire.channel;

public interface ChannelOp<TInMessage, TOutMessage> extends Channel<TOutMessage>  {

  Channel<TInMessage> getInChannel();
}
