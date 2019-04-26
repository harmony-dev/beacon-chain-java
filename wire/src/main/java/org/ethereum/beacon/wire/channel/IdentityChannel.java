package org.ethereum.beacon.wire.channel;

import java.util.function.Predicate;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public abstract class IdentityChannel<TMessage> implements ChannelOp<TMessage, TMessage> {
  private final Channel<TMessage> inChannel;

  public IdentityChannel(Channel<TMessage> inChannel) {
    this.inChannel = inChannel;
  }

  @Override
  public Channel<TMessage> getInChannel() {
    return inChannel;
  }

  @Override
  public Publisher<TMessage> inboundMessageStream() {
    return Flux.from(getInChannel().inboundMessageStream());
  }

  @Override
  public void subscribeToOutbound(Publisher<TMessage> outboundMessageStream) {
    getInChannel().subscribeToOutbound(Flux.from(outboundMessageStream));
  }

  protected void onInbound(TMessage msg) throws RuntimeException {}

  protected void onOutbound(TMessage msg)throws RuntimeException {}
}
