package org.ethereum.beacon.discovery;

import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.EnrScheme;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.handler.AuthHeaderMessagePacketHandler;
import org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouPacketHandler;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorage;
import org.ethereum.beacon.discovery.storage.NodeTableStorageFactoryImpl;
import org.ethereum.beacon.discovery.task.TaskType;
import org.javatuples.Pair;
import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.ethereum.beacon.discovery.pipeline.Field.PACKET_AUTH_HEADER_MESSAGE;
import static org.ethereum.beacon.discovery.pipeline.Field.SESSION;
import static org.ethereum.beacon.discovery.storage.NodeTableStorage.DEFAULT_SERIALIZER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HandshakeHandlersTest {
  private final BytesValue testKey1 =
      BytesValue.fromHexString("eef77acb6c6a6eebc5b363a475ac583ec7eccdb42b6481424c60f59aa326547f");
  private final BytesValue testKey2 =
      BytesValue.fromHexString("66fb62bfbd66b9177a138c1e5cddbe4f7c30c343e94e68df8769459cb1cde628");
  private Bytes32 nodeId1;
  private Bytes32 nodeId2;

  public HandshakeHandlersTest() {
    byte[] homeNodeIdBytes = new byte[32];
    homeNodeIdBytes[0] = 0x01;
    byte[] destNodeIdBytes = new byte[32];
    destNodeIdBytes[0] = 0x02;
    this.nodeId1 = Bytes32.wrap(homeNodeIdBytes);
    this.nodeId2 = Bytes32.wrap(destNodeIdBytes);
  }

  @Test
  public void authHeaderHandlerTest() throws Exception {
    // Node1
    NodeRecord nodeRecord1 =
        NodeRecordFactory.DEFAULT.createFromValues(
            EnrScheme.V4,
            UInt64.valueOf(1),
            Bytes96.EMPTY,
            Pair.with(
                NodeRecord.FIELD_IP_V4,
                Bytes4.wrap(InetAddress.getByName("127.0.0.1").getAddress())),
            Pair.with(NodeRecord.FIELD_UDP_V4, 30303),
            Pair.with(NodeRecord.FIELD_PKEY_SECP256K1, BytesValue.wrap(ECKeyPair.create(testKey1.extractArray()).getPublicKey().toByteArray())));
    // Node2
    NodeRecord nodeRecord2 =
        NodeRecordFactory.DEFAULT.createFromValues(
            EnrScheme.V4,
            UInt64.valueOf(1),
            Bytes96.EMPTY,
            Pair.with(
                NodeRecord.FIELD_IP_V4,
                Bytes4.wrap(InetAddress.getByName("127.0.0.1").getAddress())),
            Pair.with(NodeRecord.FIELD_UDP_V4, 30304),
            Pair.with(NodeRecord.FIELD_PKEY_SECP256K1, BytesValue.wrap(ECKeyPair.create(testKey2.extractArray()).getPublicKey().toByteArray())));

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
            testKey1,
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
            testKey2,
            nodeTableStorage2.get(),
            nodeBucketStorage2,
            new AuthTagRepository(),
            outgoingMessages2to1,
            rnd);
    WhoAreYouPacketHandler whoAreYouPacketHandlerNode1 = new WhoAreYouPacketHandler();
    Envelope envelopeAt1From2 = new Envelope();
    byte[] idNonceBytes = new byte[32];
    Functions.getRandom().nextBytes(idNonceBytes);
    Bytes32 idNonce = Bytes32.wrap(idNonceBytes);
    nodeSessionAt2For1.setIdNonce(idNonce);
    BytesValue authTag = nodeSessionAt2For1.generateNonce();
    authTagRepository1.put(authTag, nodeSessionAt1For2);
    envelopeAt1From2.put(
        Field.PACKET_WHOAREYOU,
        WhoAreYouPacket.create(nodeId1, authTag, idNonce, UInt64.ZERO));
    envelopeAt1From2.put(Field.SESSION, nodeSessionAt1For2);
    nodeSessionAt1For2.saveTask(TaskType.FINDNODE);
    whoAreYouPacketHandlerNode1.handle(envelopeAt1From2);
    authHeaderPacketLatch.await(1, TimeUnit.SECONDS);

    // Node2 handle AuthHeaderPacket and finish handshake
    AuthHeaderMessagePacketHandler authHeaderMessagePacketHandlerNode2 =
        new AuthHeaderMessagePacketHandler();
    Envelope envelopeAt2From1 = new Envelope();
    envelopeAt2From1.put(PACKET_AUTH_HEADER_MESSAGE, authHeaderPacket[0]);
    envelopeAt2From1.put(SESSION, nodeSessionAt2For1);
    assertFalse(nodeSessionAt2For1.isAuthenticated());
    authHeaderMessagePacketHandlerNode2.handle(envelopeAt2From1);
    assertTrue(nodeSessionAt2For1.isAuthenticated());
  }
  // 	net := newHandshakeTest()
  //	defer net.close()
  //
  //	var (
  //		idA       = net.nodeA.id()
  //		addrA     = net.nodeA.addr()
  //		challenge = &whoareyouV5{AuthTag: []byte("authresp"), RecordSeq: 0, node: net.nodeB.n()}
  //		nonce     = make([]byte, gcmNonceSize)
  //	)
  //	header, _, _ := net.nodeA.c.makeAuthHeader(nonce, challenge)
  //	challenge.node = nil // force ENR signature verification in decoder
  //	b.ResetTimer()
  //
  //	for i := 0; i < b.N; i++ {
  //		_, _, err := net.nodeB.c.decodeAuthResp(idA, addrA, header, challenge)
  //		if err != nil {
  //			b.Fatal(err)
  //		}
  //	}

  @Test
  public void testHandshake() {
    // A -> B   RANDOM PACKET
    FindNodeMessage findNodeMessage = new FindNodeMessage(BytesValue.fromHexString("01"), 10);

    // func TestHandshakeV5(t *testing.T) {
    //	t.Parallel()
    //	net := newHandshakeTest()
    //	defer net.close()
    //
    //	// A -> B   RANDOM PACKET
    //	packet, _ := net.nodeA.encode(t, net.nodeB, &findnodeV5{})
    //	resp := net.nodeB.expectDecode(t, p_unknownV5, packet)
    //
    //	// A <- B   WHOAREYOU
    //	challenge := &whoareyouV5{
    //		AuthTag:   resp.(*unknownV5).AuthTag,
    //		IDNonce:   testIDnonce,
    //		RecordSeq: 0,
    //	}
    //	whoareyou, _ := net.nodeB.encode(t, net.nodeA, challenge)
    //	net.nodeA.expectDecode(t, p_whoareyouV5, whoareyou)
    //
    //	// A -> B   FINDNODE
    //	findnode, _ := net.nodeA.encodeWithChallenge(t, net.nodeB, challenge, &findnodeV5{})
    //	net.nodeB.expectDecode(t, p_findnodeV5, findnode)
    //	if len(net.nodeB.c.handshakes) > 0 {
    //		t.Fatalf("node B didn't remove handshake from challenge map")
    //	}
    //
    //	// A <- B   NODES
    //	nodes, _ := net.nodeB.encode(t, net.nodeA, &nodesV5{Total: 1})
    //	net.nodeA.expectDecode(t, p_nodesV5, nodes)
    //
  }

  //  private BytesValue encodeWithChallenge(V5Message v5Message, WhoAreYouPacket challenge) {
  // Copy challenge and add destination node. This avoids sharing challenge among the two codecs.
  //	var challenge *whoareyouV5
  //	if c != nil {
  //		challengeCopy := *c
  //		challenge = &challengeCopy
  //		challenge.node = to.n()
  //	}
  //	// Encode to destination.
  //	enc, authTag, err := n.c.encode(to.id(), to.addr(), p, challenge)
  //	if err != nil {
  //		t.Fatal(fmt.Errorf("(%s) %v", n.ln.ID().TerminalString(), err))
  //	}
  //	t.Logf("(%s) -> (%s)   %s\n%s", n.ln.ID().TerminalString(), to.id().TerminalString(),
  // p.name(), hex.Dump(enc))
  //	return enc, authTag
  //  }

  //  private BytesValue encode(V5Message v5Message) {
  //	// Encode to destination.
  //	enc, authTag, err := n.c.encode(to.id(), to.addr(), p, challenge)
  //	return enc, authTag

  // // encode encodes a packet to a node. 'id' and 'addr' specify the destination node. The
  //// 'token' parameter should be set to the token in the most recently received WHOAREYOU
  //// packet.
  // func (c *wireCodec) encode(id enode.ID, addr *net.UDPAddr, packet packetV5, challenge
  // *whoareyouV5) ([]byte, []byte, error) {
  //	if packet.kind() == p_whoareyouV5 {
  //		p := packet.(*whoareyouV5)
  //		enc, err := c.encodeWhoareyou(id, p)
  //		if err == nil {
  //			c.storeSentHandshake(id, addr, p)
  //		}
  //		return enc, nil, err
  //	}
  //	// Ensure calling code sets node if needed.
  //	if challenge != nil && challenge.node == nil {
  //		panic("BUG: missing challenge.node in encode")
  //	}
  //	_, writeKey := c.loadKeys(id, addr)
  //	if writeKey != nil || challenge != nil {
  //		return c.encodeEncrypted(id, addr, packet, writeKey, challenge)
  //	}
  //	return c.encodeRandom(id)
  // }
  //  }
}
