package org.ethereum.beacon.wire.channel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.MessageSerializer;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.exceptions.WireRemoteRpcError;
import org.ethereum.beacon.wire.message.Message;
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
import org.ethereum.beacon.wire.message.payload.MessageType;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconPipeline {

  MessageSerializer messageSerializer;
  SSZSerializer sszSerializer;
  WireApiSync syncServer = null;

  WireApiSync syncClient;


  static class BeaconRpcMapper extends RpcChannelMapper<Message, RequestMessage, ResponseMessage> {
    private AtomicLong idGen = new AtomicLong(1);

    public BeaconRpcMapper(Channel<Message> inChannel) {
      super(inChannel);
    }

    @Override
    protected boolean isRequest(Message msg) {
      return msg instanceof RequestMessage;
    }

    @Override
    protected boolean isNotification(Message msg) {
      return MessageType.getById(((RequestMessage) msg).getMethodId()).isNotification();
    }

    @Override
    protected Object generateNextId() {
      return UInt64.valueOf(idGen.getAndIncrement());
    }

    @Override
    protected Object getId(Message msg) {
      return msg.getId();
    }

    @Override
    protected void setId(Message msg, Object id) {
      msg.setId((UInt64) id);
    }
  }

  static class BeaconPayloadCodec extends ChannelCodec<
      RpcMessage<RequestMessage, ResponseMessage>,
      RpcMessage<RequestMessagePayload, ResponseMessagePayload>> {

    private static final Object CONTEXT_REQUEST_MESSAGE_ID = new Object();

    public BeaconPayloadCodec(
        Channel<RpcMessage<RequestMessage, ResponseMessage>> inChannel,
        SSZSerializer sszSerializer) {

      super(inChannel, msg -> decode(sszSerializer, msg), msg -> encode(sszSerializer, msg));
    }

    static RpcMessage<RequestMessagePayload, ResponseMessagePayload> decode(
        SSZSerializer sszSerializer, RpcMessage<RequestMessage, ResponseMessage> msg) {

      if (msg.isRequest()) {
        MessageType messageType = MessageType.getById(msg.getRequest().getMethodId());
        RequestMessagePayload messagePayload = sszSerializer
            .decode(msg.getRequest().getBody(), messageType.getRequestClass());
        return msg.copyWithRequest(messagePayload);
      } else {
        int methodId = (int) msg.popRequestContext(CONTEXT_REQUEST_MESSAGE_ID);
        if (msg.getResponse().get().getResponseCode() == 0) {
          ResponseMessagePayload messagePayload =
              sszSerializer.decode(
                  msg.getResponse().get().getResult(),
                  MessageType.getById(methodId).getResponseClass());
          return msg.copyWithResponse(messagePayload);
        } else {
          return msg.copyWithResponseError(deserializeError(sszSerializer,
                  msg.getResponse().get().getResponseCode(), msg.getResponse().get().getResult()));
        }
      }
    }

    static  RpcMessage<RequestMessage, ResponseMessage> encode(
        SSZSerializer sszSerializer, RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg) {
      if (msg.isRequest()) {
        int methodId = msg.getRequest().getMethodId();
        msg.pushRequestContext(CONTEXT_REQUEST_MESSAGE_ID, methodId);
        BytesValue payloadBytes = sszSerializer.encode2(msg.getRequest());
        return msg.copyWithRequest(new RequestMessage(methodId, payloadBytes));
      } else {
        if (msg.getError().isPresent()) {
          return msg.copyWithResponse(serializeError(sszSerializer, msg.getError().get()));
        } else {
          BytesValue payloadBytes = sszSerializer.encode2(msg.getResponse().get());
          return msg.copyWithResponse(new ResponseMessage(0, payloadBytes));
        }
      }
    }

    protected static ResponseMessage serializeError(SSZSerializer sszSerializer, Throwable t) {
      return new ResponseMessage(0xFF, BytesValue.EMPTY);
    }

    protected static Throwable deserializeError(SSZSerializer sszSerializer, int respCode, BytesValue data) {
      return new WireRemoteRpcError("Remote peer call error: code = " + respCode + ", payload: " + data);
    }
  }

  public BeaconPipeline(SSZSerializer sszSerializer, WireApiSync syncServer) {
    this.sszSerializer = sszSerializer;
    this.syncServer = syncServer;
  }

  public WireApiSync getSyncClient() {
    return syncClient;
  }

  public void createFromBytesChannel(Channel<BytesValue> rawChannel) {

    Channel<Message> messageChannel = new ChannelCodec<>(rawChannel,
        messageSerializer::deserialize, messageSerializer::serialize);

    createFromMessageChannel(messageChannel);
  }

  public void createFromMessageChannel(Channel<Message> messageChannel) {
    RpcChannelMapper<Message, RequestMessage, ResponseMessage> rpcMessageChannel =
        new BeaconRpcMapper(messageChannel);

    ChannelCodec<
            RpcMessage<RequestMessage, ResponseMessage>,
            RpcMessage<RequestMessagePayload, ResponseMessagePayload>>
        payloadCodec = new BeaconPayloadCodec(rpcMessageChannel, sszSerializer);

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

    RpcChannel<RequestMessagePayload, ResponseMessagePayload> hub = RpcChannel.from(new ChannelHub<>(
        inboundResponsePayloadValidator));

    RpcChannelAdapter<BlockRootsRequestMessage, BlockRootsResponseMessage> blockRootsAsync =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(hub, BlockRootsRequestMessage.class),
            syncServer::requestBlockRoots);
    RpcChannelAdapter<BlockHeadersRequestMessage, BlockHeadersResponseMessage> blockHeadersAsync = null;
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(hub, BlockHeadersRequestMessage.class),
            syncServer::requestBlockHeaders);
    RpcChannelAdapter<BlockBodiesRequestMessage, BlockBodiesResponseMessage> blockBodiesAsync = null;
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(hub, BlockBodiesRequestMessage.class),
            req -> syncServer.requestBlockBodies(req).thenApply(Feedback::get));

    syncClient = new WireApiSync() {
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
