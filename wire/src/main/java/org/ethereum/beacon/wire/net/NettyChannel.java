package org.ethereum.beacon.wire.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.function.Consumer;
import org.ethereum.beacon.wire.channel.Channel;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class NettyChannel extends SimpleChannelInboundHandler<ByteBuf> implements Channel<BytesValue> {

  private final Consumer<NettyChannel> activeChannelListener;
  private final ReplayProcessor<BytesValue> inMessages = ReplayProcessor.create();
  private final FluxSink<BytesValue> inMessagesSink = inMessages.sink();
  private ChannelHandlerContext ctx;
  private Disposable outboundSubscription;

  public NettyChannel(Consumer<NettyChannel> activeChannelListener) {
    this.activeChannelListener = activeChannelListener;
  }

  @Override
  public Publisher<BytesValue> inboundMessageStream() {
    return inMessages;
  }

  @Override
  public void subscribeToOutbound(Publisher<BytesValue> outboundMessageStream) {
    outboundSubscription = Flux.from(outboundMessageStream).subscribe(this::send);
  }

  private void send(BytesValue bytesValue) {
    ByteBuf buffer = ctx.alloc().buffer(bytesValue.size());
    bytesValue.appendTo(buffer);
    ctx.writeAndFlush(buffer);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    this.ctx = ctx;
    activeChannelListener.accept(this);
    ctx.channel().closeFuture().addListener((ChannelFutureListener) future -> closed());
  }

  private void closed() {
    inMessagesSink.complete();
    if (outboundSubscription != null) {
      outboundSubscription.dispose();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    inMessagesSink.error(cause);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
    // can't do BytesValue.wrapBuffer since no control over BytesValue instance lifecycle
    byte[] copy = new byte[byteBuf.readableBytes()];
    byteBuf.readBytes(copy);
    inMessagesSink.next(BytesValue.wrap(copy));
  }
}
