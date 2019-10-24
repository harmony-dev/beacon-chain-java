package org.ethereum.beacon.wire.impl.plain.channel;

import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class ChannelCodec<TInMessage, TOutMessage> implements ChannelOp<TInMessage, TOutMessage> {

  private final Channel<TInMessage> inChannel;
  private final Function<TInMessage, TOutMessage> messageDecoder;
  private final Function<TOutMessage, TInMessage> messageEncoder;

  public ChannelCodec(Channel<TInMessage> inChannel,
      Function<TInMessage, TOutMessage> messageDecoder,
      Function<TOutMessage, TInMessage> messageEncoder) {
    this.inChannel = inChannel;
    this.messageDecoder = messageDecoder;
    this.messageEncoder = messageEncoder;
  }

  @Override
  public Publisher<TOutMessage> inboundMessageStream() {
    return Flux.from(inChannel.inboundMessageStream()).map(messageDecoder);
  }

  @Override
  public void subscribeToOutbound(Publisher<TOutMessage> outboundMessageStream) {
    inChannel.subscribeToOutbound(Flux.from(outboundMessageStream).map(messageEncoder));
  }

  @Override
  public Channel<TInMessage> getInChannel() {
    return inChannel;
  }
}
