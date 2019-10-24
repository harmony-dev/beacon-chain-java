package org.ethereum.beacon.wire.impl.plain.channel;

import java.util.function.Predicate;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class ChannelFilter<TMessage> implements ChannelOp<TMessage, TMessage> {
  private final Channel<TMessage> inChannel;
  private final Predicate<TMessage> filter;

  public ChannelFilter(Channel<TMessage> inChannel, Predicate<TMessage> filter) {
    this.inChannel = inChannel;
    this.filter = filter;
  }

  @Override
  public Channel<TMessage> getInChannel() {
    return inChannel;
  }

  @Override
  public Publisher<TMessage> inboundMessageStream() {
    return Flux.from(getInChannel().inboundMessageStream()).filter(filter);
  }

  @Override
  public void subscribeToOutbound(Publisher<TMessage> outboundMessageStream) {
    getInChannel().subscribeToOutbound(Flux.from(outboundMessageStream).filter(filter));
  }
}
