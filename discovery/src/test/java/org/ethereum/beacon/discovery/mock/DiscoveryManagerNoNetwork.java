package org.ethereum.beacon.discovery.mock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.DiscoveryManager;
import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.network.NetworkParcel;
import org.ethereum.beacon.discovery.network.NetworkParcelV5;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link DiscoveryManager} without network as an opposite to Netty network
 * implementation {@link org.ethereum.beacon.discovery.DiscoveryManagerImpl} Outgoing packets could
 * be obtained from `outgoingMessages` publisher, using {@link #getOutgoingMessages()}, incoming
 * packets could be provided through the constructor parameter `incomingPackets`
 */
public class DiscoveryManagerNoNetwork implements DiscoveryManager {
  private static final Logger logger = LogManager.getLogger(DiscoveryManager.class);
  private final ReplayProcessor<NetworkParcel> outgoingMessages = ReplayProcessor.cacheLast();
  private final FluxSink<NetworkParcel> outgoingSink = outgoingMessages.sink();
  private final Bytes32 homeNodeId;
  private final NodeRecord homeNodeRecord;
  private final NodeTable nodeTable;
  private final NodeBucketStorage nodeBucketStorage;
  private Publisher<UnknownPacket> incomingPackets;
  private Map<Bytes32, NodeContext> recentContexts = new ConcurrentHashMap<>(); // nodeId -> context
  private AuthTagRepository authTagRepo;

  public DiscoveryManagerNoNetwork(
      NodeTable nodeTable,
      NodeBucketStorage nodeBucketStorage,
      NodeRecord homeNode,
      Publisher<UnknownPacket> incomingPackets) {
    this.nodeTable = nodeTable;
    this.nodeBucketStorage = nodeBucketStorage;
    this.incomingPackets = incomingPackets;
    this.homeNodeId = homeNode.getNodeId();
    this.homeNodeRecord = (NodeRecord) nodeTable.getHomeNode();
    this.authTagRepo = new AuthTagRepository();
  }

  public void start() {
    Flux.from(incomingPackets)
        .subscribe(
            unknownPacket -> {
              if (unknownPacket.isWhoAreYouPacket(homeNodeId)) {
                Optional<NodeContext> nodeContextOptional =
                    authTagRepo.get(unknownPacket.getWhoAreYouPacket().getAuthTag());
                if (nodeContextOptional.isPresent()) {
                  nodeContextOptional.get().addIncomingEvent(unknownPacket);
                } else {
                  // TODO: ban or whatever
                }
              } else {
                Bytes32 fromNodeId = unknownPacket.getSourceNodeId(homeNodeId);
                getContext(fromNodeId)
                    .ifPresent(context -> context.addIncomingEvent(unknownPacket));
              }
            });
  }

  @Override
  public void stop() {}

  public CompletableFuture<Void> connect(NodeRecord nodeRecord) {
    if (!nodeTable.getNode(nodeRecord.getNodeId()).isPresent()) {
      nodeTable.save(NodeRecordInfo.createDefault(nodeRecord));
    }
    NodeContext context = getContext(nodeRecord.getNodeId()).get();
    return context.initiate();
  }

  private Optional<NodeContext> getContext(Bytes32 nodeId) {
    NodeContext context = recentContexts.get(nodeId);
    if (context == null) {
      Optional<NodeRecordInfo> nodeOptional = nodeTable.getNode(nodeId);
      if (!nodeOptional.isPresent()) {
        logger.trace(
            () -> String.format("Couldn't find node record for nodeId %s, ignoring", nodeId));
        return Optional.empty();
      }
      NodeRecord nodeRecord = nodeOptional.get().getNode();
      SecureRandom random = new SecureRandom();
      context =
          new NodeContext(
              nodeRecord,
              homeNodeRecord,
              nodeTable,
              nodeBucketStorage,
              authTagRepo,
              packet -> outgoingSink.next(new NetworkParcelV5(packet, nodeRecord)),
              random);
    }

    return Optional.of(context);
  }

  public Publisher<NetworkParcel> getOutgoingMessages() {
    return outgoingMessages;
  }
}
