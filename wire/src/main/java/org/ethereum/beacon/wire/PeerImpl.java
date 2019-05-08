package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.channel.BeaconPipeline;
import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.message.payload.GoodbyeMessage;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class PeerImpl implements Peer {

  private static class WireApiSubHub implements WireApiSub, WireApiSub2 {
    private final ReplayProcessor<BeaconBlock> blockProcessor = ReplayProcessor.cacheLast();
    private final FluxSink<BeaconBlock> blockSink = blockProcessor.sink();
    private final ReplayProcessor<Attestation> attestProcessor = ReplayProcessor.cacheLast();
    private final FluxSink<Attestation> attestSink = attestProcessor.sink();

    private WireApiSub2 subClient;

    public void setSubClient(WireApiSub2 subClient) {
      this.subClient = subClient;
    }

    @Override
    public void sendProposedBlock(BeaconBlock block) {
      subClient.newBlock(block);
    }

    @Override
    public void sendAttestation(Attestation attestation) {
      subClient.newAttestation(attestation);
    }

    @Override
    public Publisher<BeaconBlock> inboundBlocksStream() {
      return blockProcessor;
    }

    @Override
    public Publisher<Attestation> inboundAttestationsStream() {
      return attestProcessor;
    }

    @Override
    public void newBlock(BeaconBlock block) {
      blockSink.next(block);
    }

    @Override
    public void newAttestation(Attestation attestation) {
      attestSink.next(attestation);
    }
  }

  private final Channel<BytesValue> channel;
  private final WireApiPeer apiPeerRemote;
  private final HelloMessage localHelloMessage;
  private final CompletableFuture<HelloMessage> remoteHelloMessageFut = new CompletableFuture<>();
  private final CompletableFuture<HelloMessage> peerActiveFut = new CompletableFuture<>();
  private final BeaconPipeline beaconPipeline;
  private final WireApiSubHub wireApiSubHub = new WireApiSubHub();

  private GoodbyeMessage remoteGoodbye;
  private GoodbyeMessage localGoodbye;

  public PeerImpl(
      Channel<BytesValue> channel,
      HelloMessage helloMessage,
      SSZSerializer ssz,
      MessageSerializer messageSerializer,
      WireApiSync syncServer) {

    this.channel = channel;
    this.localHelloMessage = helloMessage;

    beaconPipeline = new BeaconPipeline(ssz, new WireApiPeer() {
      public void hello(HelloMessage message) {
        onHello(message);
      }
      public void goodbye(GoodbyeMessage message) {
        onGoodbye(message);
      }
    }, wireApiSubHub, syncServer);
    beaconPipeline.initFromBytesChannel(channel, messageSerializer);
    wireApiSubHub.setSubClient(beaconPipeline.getSubClient());

    apiPeerRemote = beaconPipeline.getPeerClient();
    apiPeerRemote.hello(helloMessage);
  }

  @Override
  public Channel<BytesValue> getRawChannel() {
    return channel;
  }

  public CompletableFuture<HelloMessage> getRemoteHelloMessage() {
    return remoteHelloMessageFut;
  }

  public CompletableFuture<HelloMessage> getPeerActiveFuture() {
    return peerActiveFut;
  }

  private void onHello(HelloMessage message) {
    remoteHelloMessageFut.complete(message);

    if (localHelloMessage.getNetworkId() != message.getNetworkId()) {
      disconnect(new GoodbyeMessage(GoodbyeMessage.IRRELEVANT_NETWORK));
    }
    if (!localHelloMessage.getChainId().equals(message.getChainId())) {
      disconnect(new GoodbyeMessage(GoodbyeMessage.IRRELEVANT_NETWORK));
    }

    peerActiveFut.complete(message);
  }

  private void onGoodbye(GoodbyeMessage message) {
    remoteGoodbye = message;
  }

  public void disconnect(GoodbyeMessage message) {
    localGoodbye = message;
    apiPeerRemote.goodbye(message);
    channel.close();
  }

  @Override
  public WireApiSync getSyncApi() {
    return beaconPipeline.getSyncClient();
  }

  @Override
  public WireApiSub getSubApi() {
    return wireApiSubHub;
  }
}
