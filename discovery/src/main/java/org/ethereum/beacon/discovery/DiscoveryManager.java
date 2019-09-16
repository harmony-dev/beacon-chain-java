package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryManager {
  private static final Logger logger = LogManager.getLogger(DiscoveryManager.class);
  private final ReplayProcessor<NetworkPacket> outgoingMessages = ReplayProcessor.cacheLast();
  private final FluxSink<NetworkPacket> outgoingSink = outgoingMessages.sink();
  private final Bytes32 homeNodeId;
  private final NodeRecordV5 homeNodeRecord;
  private NodeTable nodeTable;
  private Publisher<UnknownPacket> incomingPackets;
  private Map<Bytes32, NodeContext> recentContexts = new ConcurrentHashMap<>(); // nodeId -> context
  private AuthTagRepository authTagRepo;

  public DiscoveryManager(
      NodeTable nodeTable, Publisher<UnknownPacket> incomingPackets, Bytes32 homeNodeId) {
    this.nodeTable = nodeTable;
    this.incomingPackets = incomingPackets;
    this.homeNodeId = homeNodeId;
    this.homeNodeRecord = (NodeRecordV5) nodeTable.getHomeNode();
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

  public void connect(NodeRecord nodeRecord) {
    if (!nodeTable.getNode(nodeRecord.getNodeId()).isPresent()) {
      nodeTable.save(NodeRecordInfo.createDefault(nodeRecord));
    }
    NodeContext context = getContext(nodeRecord.getNodeId()).get();
    context.initiate();
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
      NodeRecordV5 nodeRecord = nodeOptional.get().getNode();
      SecureRandom random = new SecureRandom();
      context =
          new NodeContext(
              nodeRecord,
              homeNodeRecord,
              nodeTable,
              authTagRepo,
              packet -> outgoingSink.next(new NetworkPacket(packet, nodeRecord)),
              random);
    }

    return Optional.of(context);
  }

  public Publisher<NetworkPacket> getOutgoingMessages() {
    return outgoingMessages; // TODO: link to network
  }
}
