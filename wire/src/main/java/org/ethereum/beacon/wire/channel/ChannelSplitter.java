package org.ethereum.beacon.wire.channel;

public interface ChannelSplitter<TInMessage> {

  Channel<TInMessage> getInChannel();



}
