package org.ethereum.beacon.discovery;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.Pipeline;
import org.ethereum.beacon.discovery.pipeline.PipelineImpl;
import org.ethereum.beacon.discovery.pipeline.handler.AuthHeaderMessagePacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouPacketHandler;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorageFactoryImpl;
import org.ethereum.beacon.discovery.task.TaskType;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.ethereum.beacon.discovery.pipeline.Field.PACKET_AUTH_HEADER_MESSAGE;
import static org.ethereum.beacon.discovery.pipeline.Field.SESSION;
import static org.ethereum.beacon.discovery.storage.NodeTableStorage.DEFAULT_SERIALIZER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HandshakeHandlersTest {

  @Test
  public void authHeaderHandlerTest() throws Exception {
    // Node1
    Pair<BytesValue, NodeRecord> nodePair1 = TestUtil.generateNode(30303);
    NodeRecord nodeRecord1 = nodePair1.getValue1();
    // Node2
    Pair<BytesValue, NodeRecord> nodePair2 = TestUtil.generateNode(30304);
    NodeRecord nodeRecord2 = nodePair2.getValue1();
    Random rnd = new Random();
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database1 = Database.inMemoryDB();
    Database database2 = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage1 =
        nodeTableStorageFactory.createTable(
            database1,
            DEFAULT_SERIALIZER,
            () -> nodeRecord1,
            () ->
                new ArrayList<NodeRecord>() {
                  {
                    add(nodeRecord2);
                  }
                });
    NodeBucketStorage nodeBucketStorage1 =
        nodeTableStorageFactory.createBucketStorage(database1, DEFAULT_SERIALIZER, nodeRecord1);
    NodeTableStorage nodeTableStorage2 =
        nodeTableStorageFactory.createTable(
            database2,
            DEFAULT_SERIALIZER,
            () -> nodeRecord2,
            () ->
                new ArrayList<NodeRecord>() {
                  {
                    add(nodeRecord1);
                  }
                });
    NodeBucketStorage nodeBucketStorage2 =
        nodeTableStorageFactory.createBucketStorage(database2, DEFAULT_SERIALIZER, nodeRecord2);

    // Node1 create AuthHeaderPacket
    final Packet[] authHeaderPacket = new Packet[1];
    final CountDownLatch authHeaderPacketLatch = new CountDownLatch(1);
    final Consumer<Packet> outgoingMessages1to2 =
        packet -> {
          authHeaderPacket[0] = packet;
          authHeaderPacketLatch.countDown();
        };
    AuthTagRepository authTagRepository1 = new AuthTagRepository();
    NodeSession nodeSessionAt1For2 =
        new NodeSession(
            nodeRecord2,
            nodeRecord1,
            nodePair1.getValue0(),
            nodeTableStorage1.get(),
            nodeBucketStorage1,
            authTagRepository1,
            outgoingMessages1to2,
            rnd);
    final Consumer<Packet> outgoingMessages2to1 =
        packet -> {
          // do nothing, we don't need to test it here
        };
    NodeSession nodeSessionAt2For1 =
        new NodeSession(
            nodeRecord1,
            nodeRecord2,
            nodePair2.getValue0(),
            nodeTableStorage2.get(),
            nodeBucketStorage2,
            new AuthTagRepository(),
            outgoingMessages2to1,
            rnd);

    Scheduler taskScheduler = Schedulers.createDefault().events();
    Pipeline outgoingPipeline = new PipelineImpl();
    WhoAreYouPacketHandler whoAreYouPacketHandlerNode1 =
        new WhoAreYouPacketHandler(outgoingPipeline, taskScheduler);
    Envelope envelopeAt1From2 = new Envelope();
    byte[] idNonceBytes = new byte[32];
    Functions.getRandom().nextBytes(idNonceBytes);
    Bytes32 idNonce = Bytes32.wrap(idNonceBytes);
    nodeSessionAt2For1.setIdNonce(idNonce);
    BytesValue authTag = nodeSessionAt2For1.generateNonce();
    authTagRepository1.put(authTag, nodeSessionAt1For2);
    envelopeAt1From2.put(
        Field.PACKET_WHOAREYOU,
        WhoAreYouPacket.create(nodePair1.getValue1().getNodeId(), authTag, idNonce, UInt64.ZERO));
    envelopeAt1From2.put(Field.SESSION, nodeSessionAt1For2);
    CompletableFuture<Void> future = new CompletableFuture<>();
    nodeSessionAt1For2.createNextRequest(TaskType.FINDNODE, future);
    whoAreYouPacketHandlerNode1.handle(envelopeAt1From2);
    authHeaderPacketLatch.await(1, TimeUnit.SECONDS);

    // Node2 handle AuthHeaderPacket and finish handshake
    AuthHeaderMessagePacketHandler authHeaderMessagePacketHandlerNode2 =
        new AuthHeaderMessagePacketHandler(outgoingPipeline, taskScheduler);
    Envelope envelopeAt2From1 = new Envelope();
    envelopeAt2From1.put(PACKET_AUTH_HEADER_MESSAGE, authHeaderPacket[0]);
    envelopeAt2From1.put(SESSION, nodeSessionAt2For1);
    assertFalse(nodeSessionAt2For1.isAuthenticated());
    authHeaderMessagePacketHandlerNode2.handle(envelopeAt2From1);
    assertTrue(nodeSessionAt2For1.isAuthenticated());
  }
}
