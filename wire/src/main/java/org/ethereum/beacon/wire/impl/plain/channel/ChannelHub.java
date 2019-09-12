package org.ethereum.beacon.wire.impl.plain.channel;

import org.reactivestreams.Publisher;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

public class ChannelHub<TMessage> implements Channel<TMessage> {

  Channel<TMessage> inChannel;
  Flux<TMessage> inMessagePublisher;
  ConnectableFlux<TMessage> connectableFlux;

  public ChannelHub(Channel<TMessage> inChannel, boolean autoConnect) {
    this.inChannel = inChannel;
    connectableFlux = Flux.from(inChannel.inboundMessageStream()).publish();
    if (autoConnect) {
      inMessagePublisher = connectableFlux.autoConnect();
    } else {
      inMessagePublisher = connectableFlux;
    }
  }

  public void connect() {
    connectableFlux.connect();
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
