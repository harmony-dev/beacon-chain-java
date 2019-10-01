package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV4;
import org.ethereum.beacon.discovery.NodeStatus;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.ethereum.beacon.discovery.storage.NodeTableStorage.DEFAULT_SERIALIZER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeTableTest {

  private Supplier<NodeRecordV4> homeNodeSupplier =
      () -> {
        try {
          return NodeRecordV4.Builder.empty()
              .withIpV4Address(BytesValue.wrap(InetAddress.getByName("127.0.0.1").getAddress()))
              .withSeq(UInt64.valueOf(1))
              .withUdpPort(30303)
              .withSecp256k1(
                  BytesValue.fromHexString(
                      "0bfb48004b1698f05872cf18b1f278998ad8f7d4c135aa41f83744e7b850ab6b98"))
              .withSignature(Bytes96.EMPTY)
              .build();
        } catch (UnknownHostException e) {
          throw new RuntimeException(e);
        }
      };

  @Test
  public void testCreate() throws Exception {
    final String localhostEnr =
        "-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8";
    NodeRecordV4 nodeRecord = (NodeRecordV4) NodeRecord.fromBase64(localhostEnr);
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage =
        nodeTableStorageFactory.createTable(
            database,
            DEFAULT_SERIALIZER,
            homeNodeSupplier,
            () -> {
              List<NodeRecord> nodes = new ArrayList<>();
              nodes.add(nodeRecord);
              return nodes;
            });
    Optional<NodeRecordInfo> extendedEnr =
        nodeTableStorage.get().getNode(nodeRecord.getNodeId());
    assertTrue(extendedEnr.isPresent());
    NodeRecordInfo nodeRecord2 = extendedEnr.get();
    assertEquals(nodeRecord.getPublicKey(), nodeRecord2.getNode().getPublicKey());
    assertEquals(
        nodeTableStorage.get().getHomeNode().getNodeId(),
        homeNodeSupplier.get().getNodeId());
  }

  @Test
  public void testFind() throws Exception {
    final String localhostEnr =
        "-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8";
    NodeRecordV4 localHostNode = (NodeRecordV4) NodeRecord.fromBase64(localhostEnr);
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage =
        nodeTableStorageFactory.createTable(
            database,
            DEFAULT_SERIALIZER,
            homeNodeSupplier,
            () -> {
              List<NodeRecord> nodes = new ArrayList<>();
              nodes.add(localHostNode);
              return nodes;
            });

    // node is adjusted to be close to localhostEnr
    NodeRecordV4 closestNode =
        NodeRecordV4.Builder.empty()
            .withIpV4Address(BytesValue.wrap(InetAddress.getByName("127.0.0.2").getAddress()))
            .withSeq(UInt64.valueOf(1))
            .withUdpPort(30303)
            .withSecp256k1(
                BytesValue.fromHexString(
                    "aafb48004b1698f05872cf18b1f278998ad8f7d4c135aa41f83744e7b850ab6b98"))
            .withSignature(Bytes96.EMPTY)
            .build();
    nodeTableStorage.get().save(new NodeRecordInfo(closestNode, -1L, NodeStatus.ACTIVE, 0));
    assertEquals(
        nodeTableStorage
            .get()
            .getNode(closestNode.getNodeId())
            .get()
            .getNode()
            .getPublicKey(),
        closestNode.getPublicKey());
    NodeRecordV4 farNode =
        NodeRecordV4.Builder.empty()
            .withIpV4Address(BytesValue.wrap(InetAddress.getByName("127.0.0.3").getAddress()))
            .withSeq(UInt64.valueOf(1))
            .withUdpPort(30303)
            .withSecp256k1(
                BytesValue.fromHexString(
                    "bafb48004b1698f05872cf18b1f278998ad8f7d4c135aa41f83744e7b850ab6b98"))
            .withSignature(Bytes96.EMPTY)
            .build();
    nodeTableStorage.get().save(new NodeRecordInfo(farNode, -1L, NodeStatus.ACTIVE, 0));
    List<NodeRecordInfo> closestNodes =
        nodeTableStorage.get().findClosestNodes(closestNode.getNodeId(), 252);
    assertEquals(2, closestNodes.size());
    assertEquals(closestNodes.get(1).getNode().getPublicKey(), localHostNode.getPublicKey());
    assertEquals(closestNodes.get(0).getNode().getPublicKey(), closestNode.getPublicKey());
    List<NodeRecordInfo> farNodes =
        nodeTableStorage.get().findClosestNodes(farNode.getNodeId(), 1);
    assertEquals(1, farNodes.size());
    assertEquals(farNodes.get(0).getNode().getPublicKey(), farNode.getPublicKey());
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
