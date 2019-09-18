package org.ethereum.beacon.discovery;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.network.DiscoveryClient;
import org.ethereum.beacon.discovery.network.DiscoveryServer;
import org.ethereum.beacon.discovery.network.DiscoveryServerImpl;
import org.ethereum.beacon.discovery.network.NettyDiscoveryServer;
import org.ethereum.beacon.discovery.network.NetworkParcel;
import org.ethereum.beacon.discovery.network.NetworkParcelV5;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.util.ExpirationScheduler;
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
import java.util.concurrent.TimeUnit;

public class DiscoveryManagerImpl implements DiscoveryManager {
  private static final int CLEANUP_DELAY_SECONDS = 180;
  private static final Logger logger = LogManager.getLogger(DiscoveryManagerImpl.class);
  private final ReplayProcessor<NetworkParcel> outgoingMessages = ReplayProcessor.cacheLast();
  private final FluxSink<NetworkParcel> outgoingSink = outgoingMessages.sink();
  private final Bytes32 homeNodeId;
  private final NodeRecordV5 homeNodeRecord;
  private final NodeTable nodeTable;
  private final Map<Bytes32, NodeContext> recentContexts =
      new ConcurrentHashMap<>(); // nodeId -> context
  private ExpirationScheduler<Bytes32> contextExpirationScheduler =
      new ExpirationScheduler<>(CLEANUP_DELAY_SECONDS, TimeUnit.SECONDS);
  private final AuthTagRepository authTagRepo;
  private final DiscoveryServer discoveryServer;
  private final CompletableFuture<Void> discoveryClientAssigned;
  private final Scheduler scheduler;
  private DiscoveryClient discoveryClient;

  public DiscoveryManagerImpl(
      NodeTable nodeTable, NodeRecordV5 homeNode, Scheduler serverScheduler) {
    this.nodeTable = nodeTable;
    this.homeNodeId = homeNode.getNodeId();
    this.homeNodeRecord = (NodeRecordV5) nodeTable.getHomeNode();
    this.authTagRepo = new AuthTagRepository();
    this.scheduler = serverScheduler;
    this.discoveryServer =
        new DiscoveryServerImpl(homeNodeRecord.getIpV4address(), homeNodeRecord.getUdpPort());
    this.discoveryClientAssigned =
        ((NettyDiscoveryServer) discoveryServer)
            .useDatagramChannel(
                nioDatagramChannel ->
                    discoveryClient = new DiscoveryClient(nioDatagramChannel, outgoingMessages));
  }

  @Override
  public void start() {
    Flux.from(discoveryServer.getIncomingPackets())
        .map(UnknownPacket::new)
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
    discoveryServer.start(scheduler);
    discoveryClientAssigned.join();
  }

  @Override
  public void stop() {
    discoveryServer.stop();
  }

  @VisibleForTesting
  void connect(NodeRecord nodeRecord) {
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
              packet -> outgoingSink.next(new NetworkParcelV5(packet, nodeRecord)),
              random);
      recentContexts.put(nodeId, context);
    }

    final NodeContext contextBackup = context;
    contextExpirationScheduler.put(context.getNodeRecord().getNodeId(), () -> {
      recentContexts.remove(contextBackup.getNodeRecord().getNodeId());
      contextBackup.cleanup();
    });
    return Optional.of(context);
  }

  @VisibleForTesting
  Publisher<NetworkParcel> getOutgoingMessages() {
    return outgoingMessages;
  }
}
