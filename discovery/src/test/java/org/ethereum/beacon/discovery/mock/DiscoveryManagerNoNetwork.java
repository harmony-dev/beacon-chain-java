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
import org.ethereum.beacon.discovery.pipeline.handler.BadPacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.IncomingDataPacker;
import org.ethereum.beacon.discovery.pipeline.handler.MessageHandler;
import org.ethereum.beacon.discovery.pipeline.handler.MessagePacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NewTaskHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NextTaskHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NodeIdToSession;
import org.ethereum.beacon.discovery.pipeline.handler.NodeSessionRequestHandler;
import org.ethereum.beacon.discovery.pipeline.handler.NotExpectedIncomingPacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.OutgoingParcelHandler;
import org.ethereum.beacon.discovery.pipeline.handler.UnknownPacketTagToSender;
import org.ethereum.beacon.discovery.pipeline.handler.UnknownPacketTypeByStatus;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouAttempt;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouPacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouSessionResolver;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.discovery.task.TaskType;
import org.ethereum.beacon.schedulers.Scheduler;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;
import tech.pegasys.artemis.util.bytes.BytesValue;

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
      BytesValue homeNodePrivateKey,
      Publisher<BytesValue> incomingPackets,
      Scheduler taskScheduler) {
    AuthTagRepository authTagRepo = new AuthTagRepository();
    this.incomingPackets = incomingPackets;
    NodeIdToSession nodeIdToSession =
        new NodeIdToSession(
            homeNode,
            homeNodePrivateKey,
            nodeBucketStorage,
            authTagRepo,
            nodeTable,
            outgoingPipeline);
    incomingPipeline
        .addHandler(new IncomingDataPacker())
        .addHandler(new WhoAreYouAttempt(homeNode.getNodeId()))
        .addHandler(new WhoAreYouSessionResolver(authTagRepo))
        .addHandler(new UnknownPacketTagToSender(homeNode))
        .addHandler(nodeIdToSession)
        .addHandler(new UnknownPacketTypeByStatus())
        .addHandler(new NotExpectedIncomingPacketHandler())
        .addHandler(new WhoAreYouPacketHandler(outgoingPipeline, taskScheduler))
        .addHandler(new AuthHeaderMessagePacketHandler(outgoingPipeline, taskScheduler))
        .addHandler(new MessagePacketHandler())
        .addHandler(new MessageHandler())
        .addHandler(new BadPacketHandler());
    outgoingPipeline
        .addHandler(new OutgoingParcelHandler(outgoingSink))
        .addHandler(new NodeSessionRequestHandler())
        .addHandler(nodeIdToSession)
        .addHandler(new NewTaskHandler())
        .addHandler(new NextTaskHandler(outgoingPipeline, taskScheduler));
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
    envelope.put(Field.NODE, nodeRecord);
    CompletableFuture<Void> future = new CompletableFuture<>();
    envelope.put(Field.TASK, taskType);
    envelope.put(Field.FUTURE, future);
    outgoingPipeline.push(envelope);
    return future;
  }

  public Publisher<NetworkParcel> getOutgoingMessages() {
    return outgoingMessages;
  }
}
