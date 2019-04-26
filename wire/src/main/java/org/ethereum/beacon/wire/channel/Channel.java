package org.ethereum.beacon.wire.channel;

import org.reactivestreams.Publisher;

public interface Channel<TMessage> {

  Publisher<TMessage> inboundMessageStream();

  void subscribeToOutbound(Publisher<TMessage> outboundMessageStream);

}
