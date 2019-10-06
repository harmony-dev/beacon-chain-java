package org.ethereum.beacon.discovery.mock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.DiscoveryManager;
import org.ethereum.beacon.discovery.DiscoveryManagerImpl;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.network.NetworkParcel;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.Pipeline;
import org.ethereum.beacon.discovery.pipeline.PipelineImpl;
import org.ethereum.beacon.discovery.pipeline.handler.AuthHeaderMessagePacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.BadPacketLogger;
import org.ethereum.beacon.discovery.pipeline.handler.IncomingDataPacker;
import org.ethereum.beacon.discovery.pipeline.handler.MessageHandler;
import org.ethereum.beacon.discovery.pipeline.handler.MessagePacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NodeContextRequestHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NodeIdToContext;
import org.ethereum.beacon.discovery.pipeline.handler.NotExpectedIncomingPacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.OutgoingParcelHandler;
import org.ethereum.beacon.discovery.pipeline.handler.TaskHandler;
import org.ethereum.beacon.discovery.pipeline.handler.UnknownPacketTagToSender;
import org.ethereum.beacon.discovery.pipeline.handler.UnknownPacketTypeByStatus;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouAttempt;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouContextResolver;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouPacketHandler;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.discovery.task.TaskType;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of {@link DiscoveryManager} without network as an opposite to Netty network
 * implementation {@link org.ethereum.beacon.discovery.DiscoveryManagerImpl} Outgoing packets could
 * be obtained from `outgoingMessages` publisher, using {@link #getOutgoingMessages()}, incoming
 * packets could be provided through the constructor parameter `incomingPackets`
 */
public class DiscoveryManagerNoNetwork implements DiscoveryManager {
  private static final Logger logger = LogManager.getLogger(DiscoveryManagerImpl.class);
  private final ReplayProcessor<NetworkParcel> outgoingMessages = ReplayProcessor.cacheLast();
  private final FluxSink<NetworkParcel> outgoingSink = outgoingMessages.sink();
  private final Publisher<BytesValue> incomingPackets;
  private final Pipeline incomingPipeline = new PipelineImpl();
  private final Pipeline outgoingPipeline = new PipelineImpl();

  public DiscoveryManagerNoNetwork(
      NodeTable nodeTable,
      NodeBucketStorage nodeBucketStorage,
      NodeRecord homeNode,
      Publisher<BytesValue> incomingPackets) {
    AuthTagRepository authTagRepo = new AuthTagRepository();
    this.incomingPackets = incomingPackets;
    NodeIdToContext nodeIdToContext =
        new NodeIdToContext(homeNode, nodeBucketStorage, authTagRepo, nodeTable, outgoingPipeline);
    incomingPipeline
        .addHandler(new IncomingDataPacker())
        .addHandler(new WhoAreYouAttempt(homeNode.getNodeId()))
        .addHandler(new WhoAreYouContextResolver(authTagRepo))
        .addHandler(new UnknownPacketTagToSender(homeNode))
        .addHandler(nodeIdToContext)
        .addHandler(new UnknownPacketTypeByStatus())
        .addHandler(new NotExpectedIncomingPacketHandler())
        .addHandler(new WhoAreYouPacketHandler())
        .addHandler(new AuthHeaderMessagePacketHandler())
        .addHandler(new MessagePacketHandler())
        .addHandler(new MessageHandler())
        .addHandler(new BadPacketLogger());
    outgoingPipeline
        .addHandler(new OutgoingParcelHandler(outgoingSink))
        .addHandler(new NodeContextRequestHandler())
        .addHandler(nodeIdToContext)
        .addHandler(new TaskHandler(new SecureRandom()));
  }

  @Override
  public void start() {
    incomingPipeline.build();
    outgoingPipeline.build();
    Flux.from(incomingPackets).subscribe(incomingPipeline::push);
  }

  @Override
  public void stop() {}

  public CompletableFuture<Void> executeTask(NodeRecord nodeRecord, TaskType taskType) {
    Envelope envelope = new Envelope();
    envelope.put(Field.TASK, taskType);
    envelope.put(Field.NODE, nodeRecord);
    CompletableFuture<Void> completed = new CompletableFuture<>();
    envelope.put(Field.FUTURE, completed);
    outgoingPipeline.push(envelope);
    return completed;
  }

  public Publisher<NetworkParcel> getOutgoingMessages() {
    return outgoingMessages;
  }
}
