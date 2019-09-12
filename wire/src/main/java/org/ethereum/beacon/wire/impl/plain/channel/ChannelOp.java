package org.ethereum.beacon.wire.impl.plain.channel;

/**
 * Represents {@link Channel} operation which has inbound {@link Channel} with messages of
 * type TInMessage, maps these messages to TOutMessage type
 * and serves them as a {@link Channel} itself
 */
public interface ChannelOp<TInMessage, TOutMessage> extends Channel<TOutMessage>  {

  /**
   * Returns the source {@link Channel}
   */
  Channel<TInMessage> getInChannel();
}
