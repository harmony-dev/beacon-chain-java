package org.ethereum.beacon.discovery.community;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.NodesMessage;
import org.ethereum.beacon.discovery.message.PingMessage;
import org.ethereum.beacon.discovery.message.PongMessage;
import org.junit.Ignore;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MessageEncodingTest {
  @Test
  public void encodePing() {
    PingMessage pingMessage =
        new PingMessage(BytesValue.wrap(UInt64.valueOf(1).toBI().toByteArray()), UInt64.valueOf(1));
    assertEquals(BytesValue.fromHexString("0x01c20101"), pingMessage.getBytes());
  }

  @Test
  public void encodePong() throws Exception {
    PongMessage pongMessage =
        new PongMessage(
            BytesValue.wrap(UInt64.valueOf(1).toBI().toByteArray()),
            UInt64.valueOf(1),
            BytesValue.wrap(InetAddress.getByName("127.0.0.1").getAddress()),
            5000);
    assertEquals(BytesValue.fromHexString("0x02ca0101847f000001821388"), pongMessage.getBytes());
  }

  @Test
  public void encodeFindNode() {
    FindNodeMessage findNodeMessage =
        new FindNodeMessage(BytesValue.wrap(UInt64.valueOf(1).toBI().toByteArray()), 256);
    assertEquals(BytesValue.fromHexString("0x03c401820100"), findNodeMessage.getBytes());
  }

  @Test
  @Ignore("Until fix resolution. Rlp encoding is not the same")
  public void encodeNodes() {
    NodeRecordFactory nodeRecordFactory = NodeRecordFactory.DEFAULT;
    NodesMessage nodesMessage =
        new NodesMessage(
            BytesValue.wrap(UInt64.valueOf(1).toBI().toByteArray()),
            2,
            () -> {
              List<NodeRecord> nodeRecords = new ArrayList<>();
              nodeRecords.add(
                  nodeRecordFactory.fromBase64(
                      "-HW4QBzimRxkmT18hMKaAL3IcZF1UcfTMPyi3Q1pxwZZbcZVRI8DC5infUAB_UauARLOJtYTxaagKoGmIjzQxO2qUygBgmlkgnY0iXNlY3AyNTZrMaEDymNMrg1JrLQB2KTGtv6MVbcNEVv0AHacwUAPMljNMTg"));
              nodeRecords.add(
                  nodeRecordFactory.fromBase64(
                      "-HW4QNfxw543Ypf4HXKXdYxkyzfcxcO-6p9X986WldfVpnVTQX1xlTnWrktEWUbeTZnmgOuAY_KUhbVV1Ft98WoYUBMBgmlkgnY0iXNlY3AyNTZrMaEDDiy3QkHAxPyOgWbxp5oF1bDdlYE6dLCUUp8xfVw50jU"));
              return nodeRecords;
            },
            2);
    assertEquals(
        BytesValue.fromHexString(
            "0x04f8f80102b8f4f8f2b877f875b8401ce2991c64993d7c84c29a00bdc871917551c7d330fca2dd0d69c706596dc655448f030b98a77d4001fd46ae0112ce26d613c5a6a02a81a6223cd0c4edaa53280182696482763489736563703235366b31a103ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138b877f875b840d7f1c39e376297f81d7297758c64cb37dcc5c3beea9f57f7ce9695d7d5a67553417d719539d6ae4b445946de4d99e680eb8063f29485b555d45b7df16a1850130182696482763489736563703235366b31a1030e2cb74241c0c4fc8e8166f1a79a05d5b0dd95813a74b094529f317d5c39d235"),
        nodesMessage.getBytes());
  }
}
