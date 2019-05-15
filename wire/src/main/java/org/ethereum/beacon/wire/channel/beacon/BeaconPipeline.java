package org.ethereum.beacon.wire.channel.beacon;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.Feedback;
import org.ethereum.beacon.wire.MessageSerializer;
import org.ethereum.beacon.wire.WireApiPeer;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.channel.ChannelCodec;
import org.ethereum.beacon.wire.channel.ChannelHub;
import org.ethereum.beacon.wire.channel.IdentityChannel;
import org.ethereum.beacon.wire.channel.RpcChannel;
import org.ethereum.beacon.wire.channel.RpcChannelAdapter;
import org.ethereum.beacon.wire.channel.RpcChannelClassFilter;
import org.ethereum.beacon.wire.channel.RpcChannelMapper;
import org.ethereum.beacon.wire.channel.RpcMessage;
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
import org.ethereum.beacon.wire.message.payload.GoodbyeMessage;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.ethereum.beacon.wire.message.payload.NotifyNewAttestationMessage;
import org.ethereum.beacon.wire.message.payload.NotifyNewBlockMessage;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BeaconPipeline {
  private static final Logger logger = LogManager.getLogger(BeaconPipeline.class);

  private final SSZSerializer sszSerializer;
  private final WireApiPeer peerServer;
  private final WireApiSubRpc subServer;
  private final WireApiSync syncServer;
  private final Schedulers schedulers;
  private WireApiPeer peerClient;
  private WireApiSubRpc subClient;
  private WireApiSync syncClient;
  private Duration rpsTimeout = RpcChannelAdapter.DEFAULT_RPC_TIMEOUT;

  private RpcChannel<RequestMessagePayload, ResponseMessagePayload> rpcHub;

  public BeaconPipeline(SSZSerializer sszSerializer, WireApiPeer peerServer,
      WireApiSubRpc subServer, WireApiSync syncServer,
      Schedulers schedulers) {
    this.sszSerializer = sszSerializer;
    this.peerServer = peerServer;
    this.subServer = subServer;
    this.syncServer = syncServer;
    this.schedulers = schedulers;
  }

  public BeaconPipeline setRpsTimeout(Duration rpsTimeout) {
    this.rpsTimeout = rpsTimeout;
    return this;
  }

  public void initFromBytesChannel(Channel<BytesValue> rawChannel, MessageSerializer messageSerializer) {

    Channel<Message> messageChannel = new ChannelCodec<>(rawChannel,
        messageSerializer::deserialize, messageSerializer::serialize);

    initFromMessageChannel(messageChannel);
  }

  public void initFromMessageChannel(Channel<Message> messageChannel) {
    RpcChannelMapper<Message, RequestMessage, ResponseMessage> rpcMessageChannel =
        new BeaconRpcMapper(messageChannel);

    messageChannel.getCloseFuture().thenAccept(v -> System.out.println("### Closed"));

    ChannelCodec<
            RpcMessage<RequestMessage, ResponseMessage>,
            RpcMessage<RequestMessagePayload, ResponseMessagePayload>>
        payloadCodec = new BeaconPayloadCodec(rpcMessageChannel, sszSerializer);

    IdentityChannel<RpcMessage<RequestMessagePayload, ResponseMessagePayload>> loggerChannel =
        new IdentityChannel<RpcMessage<RequestMessagePayload, ResponseMessagePayload>>(
            payloadCodec) {
          @Override
          protected void onInbound(RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg)
              throws RuntimeException {
            logger.debug("   ==> " + (msg.isRequest() ? msg.getRequest() : msg.getResponse().get()));
          }

          @Override
          protected void onOutbound(RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg)
              throws RuntimeException {
            logger.debug(" <==   " + (msg.isRequest() ? msg.getRequest() : msg.getResponse().get()));
          }
        };

    RpcChannel<RequestMessagePayload, ResponseMessagePayload> inboundResponsePayloadValidator = RpcChannel
        .from(new IdentityChannel<RpcMessage<RequestMessagePayload, ResponseMessagePayload>>(
            loggerChannel) {
          @Override
          protected void onOutbound(RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg)
              throws RuntimeException {
            if (msg.isRequest()) {
              msg.setRequestContext("validatorRequestMessgae", msg.getRequest());
            }
          }

          @Override
          protected void onInbound(RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg)
              throws RuntimeException {
            if (msg.isResponse()) {
              RequestMessagePayload request = (RequestMessagePayload) msg
                  .getRequestContext("validatorRequestMessgae");
              ResponseMessagePayload response = msg.getResponse().get();

              // validate response against request
            }
          }
        });

    ChannelHub<RpcMessage<RequestMessagePayload, ResponseMessagePayload>> channelHub = new ChannelHub<>(
        inboundResponsePayloadValidator, false);
    rpcHub = RpcChannel.from(channelHub);
    syncClient = createWireApiSync(syncServer);
    subClient = createWireApiSub(subServer);
    peerClient = createWireApiPeer(peerServer);

    channelHub.connect();
  }

  public WireApiPeer getPeerClient() {
    return peerClient;
  }

  public WireApiSubRpc getSubClient() {
    return subClient;
  }

  public WireApiSync getSyncClient() {
    return syncClient;
  }

  private WireApiSync createWireApiSync(WireApiSync syncServer) {
    RpcChannelAdapter<BlockRootsRequestMessage, BlockRootsResponseMessage> blockRootsAsync =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(rpcHub, BlockRootsRequestMessage.class),
            syncServer != null ? syncServer::requestBlockRoots : null, schedulers.events());
    RpcChannelAdapter<BlockHeadersRequestMessage, BlockHeadersResponseMessage> blockHeadersAsync =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(rpcHub, BlockHeadersRequestMessage.class),
            syncServer != null ? syncServer::requestBlockHeaders : null, schedulers.events());
    RpcChannelAdapter<BlockBodiesRequestMessage, BlockBodiesResponseMessage> blockBodiesAsync =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(rpcHub, BlockBodiesRequestMessage.class),
            syncServer != null ? req -> syncServer.requestBlockBodies(req).thenApply(Feedback::get) : null,
            schedulers.events());

    return new WireApiSync() {
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

  private WireApiSubRpc createWireApiSub(WireApiSubRpc subServer) {
    RpcChannelAdapter<NotifyNewBlockMessage, ResponseMessagePayload> blocks =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(rpcHub, NotifyNewBlockMessage.class),
            subServer == null ? null : newBlock -> {
              subServer.newBlock(newBlock.getBlock());
              return null;
            }, schedulers.events());

    RpcChannelAdapter<NotifyNewAttestationMessage, ResponseMessagePayload> attestations =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(rpcHub, NotifyNewAttestationMessage.class),
            subServer == null ? null : newAttest -> {
              subServer.newAttestation(newAttest.getAttestation());
              return null;
            }, schedulers.events());

    return new WireApiSubRpc() {
      @Override
      public void newBlock(BeaconBlock block) {
        blocks.notifyRemote(new NotifyNewBlockMessage(block));
      }

      @Override
      public void newAttestation(Attestation attestation) {
        attestations.notifyRemote(new NotifyNewAttestationMessage(attestation));
      }
    };
  }

  private WireApiPeer createWireApiPeer(WireApiPeer peerServer) {
    RpcChannelAdapter<HelloMessage, ResponseMessagePayload> helloRpc =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(rpcHub, HelloMessage.class),
            peerServer == null ? null : msg -> {
              peerServer.hello(msg);
              return null;
            }, schedulers.events());

    RpcChannelAdapter<GoodbyeMessage, ResponseMessagePayload> goodbyeRpc =
        new RpcChannelAdapter<>(new RpcChannelClassFilter<>(rpcHub, GoodbyeMessage.class),
            peerServer == null ? null : msg -> {
              peerServer.goodbye(msg);
              return null;
            }, schedulers.events());

    return new WireApiPeer() {
      @Override
      public void hello(HelloMessage message) {
        helloRpc.notifyRemote(message);
      }

      @Override
      public void goodbye(GoodbyeMessage message) {
        goodbyeRpc.notifyRemote(message);
      }
    };
  }
}
