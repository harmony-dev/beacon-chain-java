package org.ethereum.beacon.wire.impl.plain;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.MessageSerializer;
import org.ethereum.beacon.wire.Peer;
import org.ethereum.beacon.wire.WireApiPeer;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import org.ethereum.beacon.wire.impl.plain.channel.beacon.BeaconPipeline;
import org.ethereum.beacon.wire.impl.plain.channel.beacon.WireApiSubAdapter;
import org.ethereum.beacon.wire.message.payload.GoodbyeMessage;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class PeerImpl implements Peer {

  private final Channel<BytesValue> channel;
  private final WireApiPeer apiPeerRemote;
  private final HelloMessage localHelloMessage;
  private final CompletableFuture<HelloMessage> remoteHelloMessageFut = new CompletableFuture<>();
  private final CompletableFuture<HelloMessage> peerActiveFut = new CompletableFuture<>();
  private final BeaconPipeline beaconPipeline;
  private final WireApiSubAdapter wireApiSubAdapter = new WireApiSubAdapter();
  private final Schedulers schedulers;

  private GoodbyeMessage remoteGoodbye;
  private GoodbyeMessage localGoodbye;

  public PeerImpl(
      Channel<BytesValue> channel,
      HelloMessage helloMessage,
      SSZSerializer ssz,
      MessageSerializer messageSerializer,
      WireApiSync syncServer,
      Schedulers schedulers) {

    this.channel = channel;
    this.localHelloMessage = helloMessage;
    this.schedulers = schedulers;

    beaconPipeline = new BeaconPipeline(ssz, new WireApiPeer() {
      public void hello(HelloMessage message) {
        onHello(message);
      }
      public void goodbye(GoodbyeMessage message) {
        onGoodbye(message);
      }
    }, wireApiSubAdapter, syncServer, schedulers);
    beaconPipeline.initFromBytesChannel(channel, messageSerializer);
    wireApiSubAdapter.setSubClient(beaconPipeline.getSubClient());

    apiPeerRemote = beaconPipeline.getPeerClient();
    apiPeerRemote.hello(helloMessage);
  }

  @Override
  public Channel<BytesValue> getConnection() {
    return channel;
  }

  @Override
  public CompletableFuture<HelloMessage> getRemoteHelloMessage() {
    return remoteHelloMessageFut;
  }

  public CompletableFuture<HelloMessage> getPeerActiveFuture() {
    return peerActiveFut;
  }

  private void onHello(HelloMessage message) {
    remoteHelloMessageFut.complete(message);

//    if (localHelloMessage.getNetworkId() != message.getNetworkId()) {
//      disconnect(new GoodbyeMessage(GoodbyeMessage.IRRELEVANT_NETWORK));
//    }
//    if (!localHelloMessage.getChainId().equals(message.getChainId())) {
//      disconnect(new GoodbyeMessage(GoodbyeMessage.IRRELEVANT_NETWORK));
//    }

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
    return wireApiSubAdapter;
  }

  @Override
  public String toString() {
    String bestSlot;
    try {
      bestSlot =
          getRemoteHelloMessage().isDone() ? getRemoteHelloMessage().get().getHeadSlot().toString()  : null;
    } catch (Exception e) {
      bestSlot = "(err )" + e;
    }
    return "Peer[" + channel + (bestSlot == null ? "" : ", slot: " + bestSlot) + "]";
  }
}
