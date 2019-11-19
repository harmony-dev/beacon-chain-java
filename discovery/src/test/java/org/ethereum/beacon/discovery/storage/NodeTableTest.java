package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.NodeStatus;
import org.ethereum.beacon.discovery.TestUtil;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.ethereum.beacon.discovery.TestUtil.NODE_RECORD_FACTORY_NO_VERIFICATION;
import static org.ethereum.beacon.discovery.TestUtil.TEST_SERIALIZER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeTableTest {
  final String LOCALHOST_BASE64 =
      "-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQMRo9bfkceoY0W04hSgYU5Q1R_mmq3Qp9pBPMAIduKrAYN1ZHCCdl8=";
  private Function<UInt64, NodeRecord> HOME_NODE_SUPPLIER =
      (oldSeq) -> TestUtil.generateUnverifiedNode(30303).getValue1();

  @Test
  public void testCreate() throws Exception {
    NodeRecord nodeRecord = NODE_RECORD_FACTORY_NO_VERIFICATION.fromBase64(LOCALHOST_BASE64);
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage =
        nodeTableStorageFactory.createTable(
            database,
            TEST_SERIALIZER,
            HOME_NODE_SUPPLIER,
            () -> {
              List<NodeRecord> nodes = new ArrayList<>();
              nodes.add(nodeRecord);
              return nodes;
            });
    Optional<NodeRecordInfo> extendedEnr = nodeTableStorage.get().getNode(nodeRecord.getNodeId());
    assertTrue(extendedEnr.isPresent());
    NodeRecordInfo nodeRecord2 = extendedEnr.get();
    assertEquals(
        nodeRecord.get(NodeRecord.FIELD_PKEY_SECP256K1),
        nodeRecord2.getNode().get(NodeRecord.FIELD_PKEY_SECP256K1));
    assertEquals(
        nodeTableStorage.get().getHomeNode().getNodeId(),
        HOME_NODE_SUPPLIER.apply(UInt64.ZERO).getNodeId());
  }

  @Test
  public void testFind() throws Exception {
    NodeRecord localHostNode = NODE_RECORD_FACTORY_NO_VERIFICATION.fromBase64(LOCALHOST_BASE64);
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage =
        nodeTableStorageFactory.createTable(
            database,
            TEST_SERIALIZER,
            HOME_NODE_SUPPLIER,
            () -> {
              List<NodeRecord> nodes = new ArrayList<>();
              nodes.add(localHostNode);
              return nodes;
            });

    // node is adjusted to be close to localhostEnr
    NodeRecord closestNode = TestUtil.generateUnverifiedNode(30267).getValue1();
    nodeTableStorage.get().save(new NodeRecordInfo(closestNode, -1L, NodeStatus.ACTIVE, 0));
    assertEquals(
        nodeTableStorage
            .get()
            .getNode(closestNode.getNodeId())
            .get()
            .getNode()
            .get(NodeRecord.FIELD_PKEY_SECP256K1),
        closestNode.get(NodeRecord.FIELD_PKEY_SECP256K1));
    // node is adjusted to be far from localhostEnr
    NodeRecord farNode = TestUtil.generateUnverifiedNode(30304).getValue1();
    nodeTableStorage.get().save(new NodeRecordInfo(farNode, -1L, NodeStatus.ACTIVE, 0));
    List<NodeRecordInfo> closestNodes =
        nodeTableStorage.get().findClosestNodes(closestNode.getNodeId(), 254);
    assertEquals(2, closestNodes.size());
    Set<BytesValue> publicKeys = new HashSet<>();
    closestNodes.forEach(
        n -> publicKeys.add((BytesValue) n.getNode().get(NodeRecord.FIELD_PKEY_SECP256K1)));
    assertTrue(publicKeys.contains(localHostNode.get(NodeRecord.FIELD_PKEY_SECP256K1)));
    assertTrue(publicKeys.contains(closestNode.get(NodeRecord.FIELD_PKEY_SECP256K1)));
    List<NodeRecordInfo> farNodes = nodeTableStorage.get().findClosestNodes(farNode.getNodeId(), 1);
    assertEquals(1, farNodes.size());
    assertEquals(
        farNodes.get(0).getNode().get(NodeRecord.FIELD_PKEY_SECP256K1),
        farNode.get(NodeRecord.FIELD_PKEY_SECP256K1));
  }

  /**
   * Verifies that calculated index number is in range of [0, {@link
   * NodeTableImpl#NUMBER_OF_INDEXES})
   */
  @Test
  public void testIndexCalculation() {
    Bytes32 nodeId0 =
        Bytes32.fromHexString("0000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 nodeId1a =
        Bytes32.fromHexString("0000000000000000000000000000000000000000000000000000000000000001");
    Bytes32 nodeId1b =
        Bytes32.fromHexString("1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 nodeId1s =
        Bytes32.fromHexString("1111111111111111111111111111111111111111111111111111111111111111");
    Bytes32 nodeId9s =
        Bytes32.fromHexString("9999999999999999999999999999999999999999999999999999999999999999");
    Bytes32 nodeIdfs =
        Bytes32.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    assertEquals(0, NodeTableImpl.getNodeIndex(nodeId0));
    assertEquals(0, NodeTableImpl.getNodeIndex(nodeId1a));
    assertEquals(16, NodeTableImpl.getNodeIndex(nodeId1b));
    assertEquals(17, NodeTableImpl.getNodeIndex(nodeId1s));
    assertEquals(153, NodeTableImpl.getNodeIndex(nodeId9s));
    assertEquals(255, NodeTableImpl.getNodeIndex(nodeIdfs));
  }
}
