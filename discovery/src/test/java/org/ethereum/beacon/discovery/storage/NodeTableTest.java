package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.enr.NodeStatus;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.ethereum.beacon.discovery.enr.NodeRecordV5.NODE_ID_FUNCTION;
import static org.ethereum.beacon.discovery.storage.NodeTableStorage.DEFAULT_SERIALIZER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NodeTableTest {

  private Supplier<NodeRecordV5> homeNodeSupplier =
      () -> {
        try {
          return NodeRecordV5.Builder.empty()
              .withIpV4Address(BytesValue.wrap(InetAddress.getByName("127.0.0.1").getAddress()))
              .withSeq(UInt64.valueOf(1))
              .withUdpPort(30303)
              .withSecp256k1(
                  BytesValue.fromHexString(
                      "0bfb48004b1698f05872cf18b1f278998ad8f7d4c135aa41f83744e7b850ab6b98"))
              .build();
        } catch (UnknownHostException e) {
          throw new RuntimeException(e);
        }
      };

  @Test
  public void testCreate() throws Exception {
    final String localhostEnr =
        "-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY1iXNlY3AyNTZrMaEDymNMrg1JrLQB2KTGtv6MVbcNEVv0AHacwUAPMljNMTiCaXCEfwAAAYN1ZHCCdl8=";
    NodeRecordV5 nodeRecordV5 = (NodeRecordV5) NodeRecord.fromBase64(localhostEnr);
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage =
        nodeTableStorageFactory.create(
            database,
            DEFAULT_SERIALIZER,
            homeNodeSupplier,
            () -> {
              List<NodeRecord> nodes = new ArrayList<>();
              nodes.add(nodeRecordV5);
              return nodes;
            });
    Optional<NodeRecordInfo> extendedEnr =
        nodeTableStorage.get().getNode(NODE_ID_FUNCTION.apply(nodeRecordV5));
    assertTrue(extendedEnr.isPresent());
    NodeRecordInfo nodeRecord = extendedEnr.get();
    assertEquals(nodeRecordV5.getPublicKey(), nodeRecord.getNode().getPublicKey());
    assertEquals(
        nodeTableStorage.get().getHomeNode().getNodeId(),
        NODE_ID_FUNCTION.apply(homeNodeSupplier.get()));
  }

  @Test
  public void testFind() throws Exception {
    final String localhostEnr =
        "-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY1iXNlY3AyNTZrMaEDymNMrg1JrLQB2KTGtv6MVbcNEVv0AHacwUAPMljNMTiCaXCEfwAAAYN1ZHCCdl8=";
    NodeRecordV5 localHostNode = (NodeRecordV5) NodeRecord.fromBase64(localhostEnr);
    NodeTableStorageFactoryImpl nodeTableStorageFactory = new NodeTableStorageFactoryImpl();
    Database database = Database.inMemoryDB();
    NodeTableStorage nodeTableStorage =
        nodeTableStorageFactory.create(
            database,
            DEFAULT_SERIALIZER,
            homeNodeSupplier,
            () -> {
              List<NodeRecord> nodes = new ArrayList<>();
              nodes.add(localHostNode);
              return nodes;
            });

    // node is adjusted to be close to localhostEnr
    NodeRecordV5 closestNode =
        NodeRecordV5.Builder.empty()
            .withIpV4Address(BytesValue.wrap(InetAddress.getByName("127.0.0.2").getAddress()))
            .withSeq(UInt64.valueOf(1))
            .withUdpPort(30303)
            .withSecp256k1(
                BytesValue.fromHexString(
                    "aafb48004b1698f05872cf18b1f278998ad8f7d4c135aa41f83744e7b850ab6b98"))
            .build();
    nodeTableStorage.get().save(new NodeRecordInfo(closestNode, -1L, NodeStatus.ACTIVE, 0));
    assertEquals(
        nodeTableStorage
            .get()
            .getNode(NODE_ID_FUNCTION.apply(closestNode))
            .get()
            .getNode()
            .getPublicKey(),
        closestNode.getPublicKey());
    NodeRecordV5 farNode =
        NodeRecordV5.Builder.empty()
            .withIpV4Address(BytesValue.wrap(InetAddress.getByName("127.0.0.3").getAddress()))
            .withSeq(UInt64.valueOf(1))
            .withUdpPort(30303)
            .withSecp256k1(
                BytesValue.fromHexString(
                    "bafb48004b1698f05872cf18b1f278998ad8f7d4c135aa41f83744e7b850ab6b98"))
            .build();
    nodeTableStorage.get().save(new NodeRecordInfo(farNode, -1L, NodeStatus.ACTIVE, 0));
    List<NodeRecordInfo> closestNodes =
        nodeTableStorage.get().findClosestNodes(NODE_ID_FUNCTION.apply(closestNode), 252);
    assertEquals(2, closestNodes.size());
    assertEquals(closestNodes.get(1).getNode().getPublicKey(), localHostNode.getPublicKey());
    assertEquals(closestNodes.get(0).getNode().getPublicKey(), closestNode.getPublicKey());
    List<NodeRecordInfo> farNodes =
        nodeTableStorage.get().findClosestNodes(NODE_ID_FUNCTION.apply(farNode), 1);
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
