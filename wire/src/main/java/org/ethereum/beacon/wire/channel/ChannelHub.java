package org.ethereum.beacon.wire.channel;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class ChannelHub<TMessage> implements Channel<TMessage> {

  Channel<TMessage> inChannel;
  Flux<TMessage> inMessagePublisher;

  public ChannelHub(Channel<TMessage> inChannel) {
    this.inChannel = inChannel;
    inMessagePublisher = Flux.from(inChannel.inboundMessageStream()).publish().autoConnect();
  }

  @Override
  public Publisher<TMessage> inboundMessageStream() {
    return inMessagePublisher;
  }

  @Override
  public void subscribeToOutbound(Publisher<TMessage> outboundMessageStream) {
    inChannel.subscribeToOutbound(outboundMessageStream);
  }
}
