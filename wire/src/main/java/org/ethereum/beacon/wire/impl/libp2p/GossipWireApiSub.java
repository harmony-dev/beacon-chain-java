package org.ethereum.beacon.wire.impl.libp2p;

import io.libp2p.core.crypto.PrivKey;
import io.libp2p.core.pubsub.MessageApi;
import io.libp2p.core.pubsub.PubsubApi;
import io.libp2p.core.pubsub.PubsubPublisherApi;
import io.libp2p.core.pubsub.PubsubSubscription;
import io.libp2p.core.pubsub.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Random;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.WireApiSub;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxSink;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class GossipWireApiSub implements WireApiSub {
  private final SSZSerializer sszSerializer;
  private final PubsubApi gossip;
  private final PubsubPublisherApi publisher;
  private final Topic blocksTopic = new Topic("/eth2/beacon_block/ssz");
  private final Topic attestationsTopic = new Topic("/eth2/beacon_attestation/ssz");
  private final PubsubSubscription subscription;
  private final EmitterProcessor<SignedBeaconBlock> blocksStream = EmitterProcessor.create();
  private final FluxSink<SignedBeaconBlock> blocksSink = blocksStream.sink();
  private final EmitterProcessor<Attestation> attestationsStream = EmitterProcessor.create();
  private final FluxSink<Attestation> attestationsSink = attestationsStream.sink();

  public GossipWireApiSub(SSZSerializer sszSerializer, PubsubApi gossip,
      PrivKey publisherKey) {
    this.sszSerializer = sszSerializer;
    this.gossip = gossip;
    subscription = gossip.subscribe(this::onNewMessage, blocksTopic, attestationsTopic);
    publisher = gossip.createPublisher(publisherKey, new Random().nextLong());
  }

  private void onNewMessage(MessageApi msg) {
    if (msg.getTopics().contains(blocksTopic)) {
      SignedBeaconBlock block = sszSerializer
          .decode(BytesValue.wrapBuffer(msg.getData()), SignedBeaconBlock.class);
      blocksSink.next(block);
    } else if (msg.getTopics().contains(attestationsTopic)) {
      Attestation attest = sszSerializer
          .decode(BytesValue.wrapBuffer(msg.getData()), Attestation.class);
      attestationsSink.next(attest);
    }
  }

  @Override
  public void sendProposedBlock(SignedBeaconBlock block) {
    byte[] bytes = sszSerializer.encode(block);
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    publisher.publish(byteBuf, blocksTopic);
  }

  @Override
  public void sendAttestation(Attestation attestation) {
    byte[] bytes = sszSerializer.encode(attestation);
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    publisher.publish(byteBuf, attestationsTopic);
  }

  @Override
  public Publisher<SignedBeaconBlock> inboundBlocksStream() {
    return blocksStream;
  }

  @Override
  public Publisher<Attestation> inboundAttestationsStream() {
    return attestationsStream;
  }
}
