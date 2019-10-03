package org.ethereum.beacon.discovery;

import com.google.common.annotations.VisibleForTesting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.network.DiscoveryClient;
import org.ethereum.beacon.discovery.network.DiscoveryClientImpl;
import org.ethereum.beacon.discovery.network.DiscoveryServer;
import org.ethereum.beacon.discovery.network.DiscoveryServerImpl;
import org.ethereum.beacon.discovery.network.NetworkParcel;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.Pipeline;
import org.ethereum.beacon.discovery.pipeline.PipelineImpl;
import org.ethereum.beacon.discovery.pipeline.handler.IncomingDataHandler;
import org.ethereum.beacon.discovery.pipeline.handler.IncomingPacketContextHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NodeContextRequestHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NodeIdContextHandler;
import org.ethereum.beacon.discovery.pipeline.handler.OutgoingParcelHandler;
import org.ethereum.beacon.discovery.pipeline.handler.TaskHandler;
import org.ethereum.beacon.discovery.pipeline.handler.UnknownPacketContextHandler;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouContextHandler;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouHandler;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.schedulers.Scheduler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.Bytes4;

import java.util.concurrent.CompletableFuture;

import static org.ethereum.beacon.discovery.pipeline.handler.NodeContextRequestHandler.NODE;
import static org.ethereum.beacon.discovery.pipeline.handler.TaskHandler.FUTURE;
import static org.ethereum.beacon.discovery.pipeline.handler.TaskHandler.PING;
import static org.ethereum.beacon.discovery.pipeline.handler.TaskHandler.TASK;

public class DiscoveryManagerImpl implements DiscoveryManager {
  private static final Logger logger = LogManager.getLogger(DiscoveryManagerImpl.class);
  private final ReplayProcessor<NetworkParcel> outgoingMessages = ReplayProcessor.cacheLast();
  private final FluxSink<NetworkParcel> outgoingSink = outgoingMessages.sink();
  private final DiscoveryServer discoveryServer;
  private final Scheduler scheduler;
  private final Pipeline incomingPipeline = new PipelineImpl();
  private final Pipeline outgoingPipeline = new PipelineImpl();
  private DiscoveryClient discoveryClient;

  public DiscoveryManagerImpl(
      NodeTable nodeTable,
      NodeBucketStorage nodeBucketStorage,
      NodeRecord homeNode,
      Scheduler serverScheduler,
      Scheduler clientScheduler) {
    AuthTagRepository authTagRepo = new AuthTagRepository();
    this.scheduler = serverScheduler;
    this.discoveryServer =
        new DiscoveryServerImpl(
            ((Bytes4) homeNode.get(NodeRecord.FIELD_IP_V4)),
            (int) homeNode.get(NodeRecord.FIELD_UDP_V4));
    this.discoveryClient = new DiscoveryClientImpl(outgoingMessages, clientScheduler);
    NodeIdContextHandler nodeIdContextHandler =
        new NodeIdContextHandler(
            homeNode, nodeBucketStorage, authTagRepo, nodeTable, outgoingPipeline);
    incomingPipeline
        .addHandler(new IncomingDataHandler())
        .addHandler(new WhoAreYouHandler(homeNode.getNodeId()))
        .addHandler(new WhoAreYouContextHandler(authTagRepo))
        .addHandler(new UnknownPacketContextHandler(homeNode))
        .addHandler(nodeIdContextHandler)
        .addHandler(new IncomingPacketContextHandler());
    outgoingPipeline
        .addHandler(new OutgoingParcelHandler(outgoingSink))
        .addHandler(new NodeContextRequestHandler())
        .addHandler(nodeIdContextHandler)
        .addHandler(new TaskHandler());
  }

  @Override
  public void start() {
    incomingPipeline.build();
    outgoingPipeline.build();
    Flux.from(discoveryServer.getIncomingPackets()).subscribe(incomingPipeline::send);
    discoveryServer.start(scheduler);
  }

  @Override
  public void stop() {
    discoveryServer.stop();
  }

  public CompletableFuture<Void> connect(NodeRecord nodeRecord) {
    Envelope envelope = new Envelope();
    envelope.put(TASK, PING);
    envelope.put(NODE, nodeRecord);
    CompletableFuture<Void> completed = new CompletableFuture<>();
    envelope.put(FUTURE, completed);
    outgoingPipeline.send(envelope);
    return completed;
  }

  @VisibleForTesting
  Publisher<NetworkParcel> getOutgoingMessages() {
    return outgoingMessages;
  }
}
