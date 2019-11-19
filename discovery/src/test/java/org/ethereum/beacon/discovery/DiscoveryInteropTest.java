package org.ethereum.beacon.discovery;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorageFactoryImpl;
import org.ethereum.beacon.discovery.task.TaskType;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.ethereum.beacon.discovery.TestUtil.NODE_RECORD_FACTORY_NO_VERIFICATION;
import static org.ethereum.beacon.discovery.TestUtil.TEST_SERIALIZER;

/** Same as {@link DiscoveryNoNetworkTest} but using real network */
@Ignore("Finish me!!!, then ignore because it takes too long and requires geth docker")
public class DiscoveryInteropTest {
  @Test
  public void test() throws Exception {
    // 1) start 2 nodes
    Pair<BytesValue, NodeRecord> nodePair1 = TestUtil.generateNode(40412, true);
    System.out.println(String.format("Node %s started", nodePair1.getValue1().getNodeId()));
    NodeRecord nodeRecord1 = nodePair1.getValue1();
    NodeRecord nodeRecord2 =
        NODE_RECORD_FACTORY_NO_VERIFICATION.fromBase64(
            "-IS4QHa5-0-OmPRchyyBf9jHIWnQlZXthveUPp5_DoDnMMB0V9ChlzNq_fhFixvIr8xOQcKrYsWjjeIBoUIS8HSuWbgBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQMOLLdCQcDE_I6BZvGnmgXVsN2VgTp0sJRSnzF9XDnSNYN1ZHCCdl8"); // Geth node
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database1 = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage1 =
        nodeTableStorageFactory.createTable(
            database1,
            TEST_SERIALIZER,
            (oldSeq) -> nodeRecord1,
            () ->
                new ArrayList<NodeRecord>() {
                  {
                    add(nodeRecord2);
                  }
                });
    NodeBucketStorage nodeBucketStorage1 =
        nodeTableStorageFactory.createBucketStorage(database1, TEST_SERIALIZER, nodeRecord1);
    DiscoveryManagerImpl discoveryManager1 =
        new DiscoveryManagerImpl(
            nodeTableStorage1.get(),
            nodeBucketStorage1,
            nodeRecord1,
            nodePair1.getValue0(),
            NODE_RECORD_FACTORY_NO_VERIFICATION,
            Schedulers.createDefault().newSingleThreadDaemon("server-1"),
            Schedulers.createDefault().newSingleThreadDaemon("tasks-1"));

    // 3) Expect standard 1 => 2 dialog
    CountDownLatch randomSent1to2 = new CountDownLatch(1);
    CountDownLatch whoareyouSent2to1 = new CountDownLatch(1);
    CountDownLatch authPacketSent1to2 = new CountDownLatch(1);
    CountDownLatch nodesSent2to1 = new CountDownLatch(1);

    Flux.from(discoveryManager1.getOutgoingMessages())
        .map(p -> new UnknownPacket(p.getPacket().getBytes()))
        .subscribe(
            networkPacket -> {
              // 1 -> 2 random
              if (randomSent1to2.getCount() != 0) {
                RandomPacket randomPacket = networkPacket.getRandomPacket();
                System.out.println("1 => 2: " + randomPacket);
                randomSent1to2.countDown();
              } else if (authPacketSent1to2.getCount() != 0) {
                // 1 -> 2 auth packet with FINDNODES
                AuthHeaderMessagePacket authHeaderMessagePacket =
                    networkPacket.getAuthHeaderMessagePacket();
                System.out.println("1 => 2: " + authHeaderMessagePacket);
                authPacketSent1to2.countDown();
              } else {
                throw new RuntimeException("Not expected!");
              }
            });

    // TODO: check that we receive correct nodes

    // 4) fire 1 to 2 dialog
    discoveryManager1.start();
    discoveryManager1.executeTask(nodeRecord2, TaskType.FINDNODE);

    assert randomSent1to2.await(1, TimeUnit.SECONDS);
    //    assert whoareyouSent2to1.await(1, TimeUnit.SECONDS);
    //    int distance1To2 = Functions.logDistance(nodeRecord1.getNodeId(),
    // nodeRecord2.getNodeId());
    //    assertFalse(nodeBucketStorage1.get(distance1To2).isPresent());
    //    assert authPacketSent1to2.await(1, TimeUnit.SECONDS);
    //    assert nodesSent2to1.await(1, TimeUnit.SECONDS);
    Thread.sleep(1500);
    // 1 sent findnodes to 2, received only (2) in answer, because 3 is not checked
    // 1 added 2 to its nodeBuckets, because its now checked, but not before
    //    NodeBucket bucketAt1With2 = nodeBucketStorage1.get(distance1To2).get();
    //    assertEquals(2, bucketAt1With2.size());
    //    assertEquals(
    //        nodeRecord2.getNodeId(),
    // bucketAt1With2.getNodeRecords().get(0).getNode().getNodeId());
  }
}
