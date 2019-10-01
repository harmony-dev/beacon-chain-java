package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.enr.NodeStatus;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.stream.IntStream;

import static org.ethereum.beacon.discovery.storage.NodeTableStorage.DEFAULT_SERIALIZER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeBucketTest {
  private final Random rnd = new Random();

  private NodeRecordInfo generateUniqueRecord() {
    try {
      byte[] pkey = new byte[33];
      rnd.nextBytes(pkey);
      NodeRecordV5 nodeRecordV5 =
          NodeRecordV5.Builder.empty()
              .withIpV4Address(BytesValue.wrap(InetAddress.getByName("127.0.0.1").getAddress()))
              .withSeq(UInt64.valueOf(1))
              .withUdpPort(30303)
              .withSecp256k1(BytesValue.wrap(pkey))
              .build();
      return new NodeRecordInfo(nodeRecordV5, (long) rnd.nextInt(1000), NodeStatus.ACTIVE, 0);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testBucket() {
    NodeBucket nodeBucket = new NodeBucket();
    IntStream.range(0, 20).forEach(value -> nodeBucket.put(generateUniqueRecord()));
    assertEquals(NodeBucket.K, nodeBucket.size());
    assertEquals(NodeBucket.K, nodeBucket.getNodeRecords().size());

    long lastRetrySaved = -1L;
    for (NodeRecordInfo nodeRecordInfo : nodeBucket.getNodeRecords()) {
      assert nodeRecordInfo.getLastRetry()
          >= lastRetrySaved; // Assert sorted by last retry, latest retry in the end
      lastRetrySaved = nodeRecordInfo.getLastRetry();
    }
    NodeRecordInfo willNotInsertNode =
        new NodeRecordInfo(generateUniqueRecord().getNode(), -2L, NodeStatus.ACTIVE, 0);
    nodeBucket.put(willNotInsertNode);
    assertFalse(nodeBucket.contains(willNotInsertNode));
    NodeRecordInfo willInsertNode =
        new NodeRecordInfo(generateUniqueRecord().getNode(), 1001L, NodeStatus.ACTIVE, 0);
    NodeRecordInfo top =
        nodeBucket.getNodeRecords().get(NodeBucket.K - 1); // latest retry should be kept
    NodeRecordInfo bottom = nodeBucket.getNodeRecords().get(0);
    nodeBucket.put(willInsertNode);
    assertTrue(nodeBucket.contains(willInsertNode));
    assertTrue(nodeBucket.contains(top));
    assertFalse(nodeBucket.contains(bottom));
    NodeRecordInfo willInsertNode2 =
        new NodeRecordInfo(willInsertNode.getNode(), 1002L, NodeStatus.ACTIVE, 0);
    nodeBucket.put(willInsertNode2); // replaces willInsertNode with better last retry
    assertEquals(willInsertNode2, nodeBucket.getNodeRecords().get(NodeBucket.K - 1));
    NodeRecordInfo willNotInsertNode3 =
        new NodeRecordInfo(willInsertNode.getNode(), 999L, NodeStatus.ACTIVE, 0);
    nodeBucket.put(willNotInsertNode3); // does not replace willInsertNode with worse last retry
    assertEquals(willInsertNode2, nodeBucket.getNodeRecords().get(NodeBucket.K - 1)); // still 2nd
    assertEquals(top, nodeBucket.getNodeRecords().get(NodeBucket.K - 2));

    NodeRecordInfo willInsertNodeDead =
        new NodeRecordInfo(willInsertNode.getNode(), 1001L, NodeStatus.DEAD, 0);
    nodeBucket.put(willInsertNodeDead); // removes willInsertNode
    assertEquals(NodeBucket.K - 1, nodeBucket.size());
    assertFalse(nodeBucket.contains(willInsertNode));
  }

  @Test
  public void testStorage() {
    NodeRecordInfo initial = generateUniqueRecord();
    Database database = Database.inMemoryDB();
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    NodeBucketStorage nodeBucketStorage =
        nodeTableStorageFactory.createBuckets(
            database, DEFAULT_SERIALIZER, initial.getNode().getNodeId());

    for (int i = 0; i < 20; ) {
      NodeRecordInfo nodeRecordInfo = generateUniqueRecord();
      if (Functions.logDistance(initial.getNode().getNodeId(), nodeRecordInfo.getNode().getNodeId())
          == 255) {
        nodeBucketStorage.put(nodeRecordInfo);
        ++i;
      }
    }
    for (int i = 0; i < 3; ) {
      NodeRecordInfo nodeRecordInfo = generateUniqueRecord();
      if (Functions.logDistance(initial.getNode().getNodeId(), nodeRecordInfo.getNode().getNodeId())
          == 254) {
        nodeBucketStorage.put(nodeRecordInfo);
        ++i;
      }
    }
    assertEquals(16, nodeBucketStorage.get(255).get().size());
    assertEquals(3, nodeBucketStorage.get(254).get().size());
    assertFalse(nodeBucketStorage.get(253).isPresent());
    assertFalse(nodeBucketStorage.get(256).isPresent());
  }
}
