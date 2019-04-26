package org.ethereum.beacon.wire.channel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.MessageSerializer;
import org.ethereum.beacon.wire.PayloadSerializer;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.message.Message;
import org.ethereum.beacon.wire.message.MessagePayload;
import org.ethereum.beacon.wire.message.RequestMessage;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import org.ethereum.beacon.wire.message.ResponseMessage;
import org.ethereum.beacon.wire.message.ResponseMessagePayload;
import org.ethereum.beacon.wire.message.payload.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BeaconPipeline {

  Channel<BytesValue> rawChannel;
  MessageSerializer messageSerializer;
  PayloadSerializer payloadSerializer;

  void create() {
    Channel<Message> messageChannel = new ChannelCodec<>(rawChannel,
        messageSerializer::deserialize, messageSerializer::serialize);
    RpcChannelMapper<Message, RequestMessage, ResponseMessage>  rpcMessageChannel = null; // = TODO
    ChannelCodec<
            RpcMessage<RequestMessage, ResponseMessage>,
            RpcMessage<RequestMessagePayload, ResponseMessagePayload>>
        payloadCodec = null; // TODO
    //    new ChannelCodec<>(rpcMessageChannel, msg -> {
    //          if (msg.isRequest()) {
    //            return msg.copyWithRequest(
    //                (RequestMessagePayload)
    // payloadSerializer.deserialize(msg.getRequest().getBody()));
    //          } else {
    //            return msg.copyWithResponse((ResponseMessagePayload<RequestMessagePayload>)
    // payloadSerializer
    //                        .deserialize(msg.getResponse().get().getResult()));
    //          }
    //        }, msg -> {
    //      if (msg.isRequest()) {
    //        return new
    // RequestMessage()msg.copyWithRequest(payloadSerializer.serialize(msg.getRequest()));
    //      } else {
    //        return msg.copyWithResponse(payloadSerializer.serialize(msg.getResponse().get()));
    //      }
    //    });
    RpcChannel<RequestMessagePayload, ResponseMessagePayload> inboundResponsePayloadValidator = RpcChannel
        .from(new IdentityChannel<RpcMessage<RequestMessagePayload, ResponseMessagePayload>>(
            payloadCodec) {
          @Override
          protected void onOutbound(RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg)
              throws RuntimeException {
            if (msg.isRequest()) {
              msg.pushRequestContext("validatorRequestMessgae", msg.getRequest());
            }
          }

          @Override
          protected void onInbound(RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg)
              throws RuntimeException {
            if (msg.isResponse()) {
              RequestMessagePayload request = (RequestMessagePayload) msg
                  .popRequestContext("validatorRequestMessgae");
              ResponseMessagePayload response = msg.getResponse().get();

              // validate response against request
            }
          }
        });

    WireApiSync syncServer = null;

    RpcChannelAdapter<BlockRootsRequestMessage, BlockRootsResponseMessage> blockRootsAsync =
        new RpcChannelAdapter<>(RpcChannel.from(
            new RpcChannelClassFilter<>(inboundResponsePayloadValidator, BlockRootsRequestMessage.class)),
            syncServer::requestBlockRoots);
    RpcChannelAdapter<BlockHeadersRequestMessage, BlockHeadersResponseMessage> blockHeadersAsync =
        new RpcChannelAdapter<>(RpcChannel.from(
            new RpcChannelClassFilter<>(inboundResponsePayloadValidator, BlockHeadersRequestMessage.class)),
            syncServer::requestBlockHeaders);
    RpcChannelAdapter<BlockBodiesRequestMessage, BlockBodiesResponseMessage> blockBodiesAsync =
        new RpcChannelAdapter<>(RpcChannel.from(
            new RpcChannelClassFilter<>(inboundResponsePayloadValidator, BlockBodiesRequestMessage.class)),
            req -> syncServer.requestBlockBodies(req).thenApply(Feedback::get));

    WireApiSync syncClient = new WireApiSync() {
      @Override
      public CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
          BlockRootsRequestMessage requestMessage) {
        return blockRootsAsync.invokeRemote(requestMessage);
      }

      @Override
      public CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
          BlockHeadersRequestMessage requestMessage) {
        return blockHeadersAsync.invokeRemote(requestMessage);
      }

      @Override
      public CompletableFuture<Feedback<BlockBodiesResponseMessage>> requestBlockBodies(
          BlockBodiesRequestMessage requestMessage) {
        return blockBodiesAsync.invokeRemote(requestMessage).thenApply(Feedback::of);
      }
    };
  }
}
