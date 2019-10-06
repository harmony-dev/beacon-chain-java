package org.ethereum.beacon.discovery;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.EnrScheme;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.NodesMessage;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.task.TaskType;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorageFactoryImpl;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.junit.Test;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.ethereum.beacon.discovery.task.TaskMessageFactory.DEFAULT_DISTANCE;
import static org.ethereum.beacon.discovery.storage.NodeTableStorage.DEFAULT_SERIALIZER;

/** Same as {@link DiscoveryNoNetworkTest} but using real network */
public class DiscoveryNetworkTest {
  private static final NodeRecordFactory NODE_RECORD_FACTORY = NodeRecordFactory.DEFAULT;

  @Test
  public void test() throws Exception {
    // 1) start 2 nodes
    NodeRecord nodeRecord1 =
        NODE_RECORD_FACTORY.createFromValues(
            EnrScheme.V4,
            UInt64.valueOf(1),
            Bytes96.EMPTY,
            new ArrayList<Pair<String, Object>>() {
              {
                add(
                    Pair.with(
                        NodeRecord.FIELD_IP_V4,
                        Bytes4.wrap(InetAddress.getByName("127.0.0.1").getAddress())));
                add(Pair.with(NodeRecord.FIELD_UDP_V4, 30303));
                add(
                    Pair.with(
                        NodeRecord.FIELD_PKEY_SECP256K1,
                        BytesValue.fromHexString(
                            "0bfb48004b1698f05872cf18b1f278998ad8f7d4c135aa41f83744e7b850ab6b98")));
              }
            });
    NodeRecord nodeRecord2 =
        NODE_RECORD_FACTORY.createFromValues(
            EnrScheme.V4,
            UInt64.valueOf(1),
            Bytes96.EMPTY,
            new ArrayList<Pair<String, Object>>() {
              {
                add(
                    Pair.with(
                        NodeRecord.FIELD_IP_V4,
                        Bytes4.wrap(InetAddress.getByName("127.0.0.1").getAddress())));
                add(Pair.with(NodeRecord.FIELD_UDP_V4, 30304));
                add(
                    Pair.with(
                        NodeRecord.FIELD_PKEY_SECP256K1,
                        BytesValue.fromHexString(
                            "7ef3502240a42891771de732f5ee6bee3eb881939edf3e6008c0d07b502756e426")));
              }
            });
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
        nodeTableStorageFactory.createBuckets(
            database1, DEFAULT_SERIALIZER, nodeRecord1.getNodeId());
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
        nodeTableStorageFactory.createBuckets(
            database2, DEFAULT_SERIALIZER, nodeRecord2.getNodeId());
    DiscoveryManagerImpl discoveryManager1 =
        new DiscoveryManagerImpl(
            nodeTableStorage1.get(),
            nodeBucketStorage1,
            nodeRecord1,
            Schedulers.createDefault().newSingleThreadDaemon("server-1"),
            Schedulers.createDefault().newSingleThreadDaemon("client-1"));
    DiscoveryManagerImpl discoveryManager2 =
        new DiscoveryManagerImpl(
            nodeTableStorage2.get(),
            nodeBucketStorage2,
            nodeRecord2,
            Schedulers.createDefault().newSingleThreadDaemon("server-2"),
            Schedulers.createDefault().newSingleThreadDaemon("client-2"));

    discoveryManager1.start();
    discoveryManager2.start();

    // 3) Expect standard 1 => 2 dialog
    CountDownLatch randomSent1to2 = new CountDownLatch(1);
    CountDownLatch authPacketSent1to2 = new CountDownLatch(1);
    CountDownLatch whoareyouSent2to1 = new CountDownLatch(1);
    CountDownLatch findNodeSent2to1 = new CountDownLatch(1);
    CountDownLatch nodesSent1to2 = new CountDownLatch(1);
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
                // 1 -> 2 NODES packet
                MessagePacket messagePacket = networkPacket.getMessagePacket();
                System.out.println("1 => 2: " + messagePacket);
                DiscoveryMessage discoveryMessage = messagePacket.getMessage();
                assert IdentityScheme.V5.equals(discoveryMessage.getIdentityScheme());
                DiscoveryV5Message discoveryV5Message = (DiscoveryV5Message) discoveryMessage;
                NodesMessage nodesMessage = (NodesMessage) discoveryV5Message.create();
                assert 1 == nodesMessage.getTotal();
                assert 1 == nodesMessage.getNodeRecordsSize();
                assert 1 == nodesMessage.getNodeRecords().size();
                nodesSent1to2.countDown();
              }
            });
    Flux.from(discoveryManager2.getOutgoingMessages())
        .map(p -> new UnknownPacket(p.getPacket().getBytes()))
        .subscribe(
            networkPacket -> {
              // 2 -> 1 whoareyou
              if (whoareyouSent2to1.getCount() != 0) {
                WhoAreYouPacket whoAreYouPacket = networkPacket.getWhoAreYouPacket();
                System.out.println("2 => 1: " + whoAreYouPacket);
                whoareyouSent2to1.countDown();
              } else {
                // 2 -> 1 findNode
                MessagePacket messagePacket = networkPacket.getMessagePacket();
                System.out.println("2 => 1: " + messagePacket);
                DiscoveryMessage discoveryMessage = messagePacket.getMessage();
                assert IdentityScheme.V5.equals(discoveryMessage.getIdentityScheme());
                DiscoveryV5Message discoveryV5Message = (DiscoveryV5Message) discoveryMessage;
                FindNodeMessage findNodeMessage = (FindNodeMessage) discoveryV5Message.create();
                assert DEFAULT_DISTANCE == findNodeMessage.getDistance();
                findNodeSent2to1.countDown();
              }
            });

    // 4) fire 1 to 2 dialog
    discoveryManager1.executeTask(nodeRecord2, TaskType.FINDNODE);

    assert randomSent1to2.await(1, TimeUnit.SECONDS);
    assert whoareyouSent2to1.await(1, TimeUnit.SECONDS);
    assert authPacketSent1to2.await(1, TimeUnit.SECONDS);
    assert findNodeSent2to1.await(1, TimeUnit.SECONDS);
    assert nodesSent1to2.await(1, TimeUnit.SECONDS);
  }

  // TODO: discovery tasks are emitted from time to time as they should
}
