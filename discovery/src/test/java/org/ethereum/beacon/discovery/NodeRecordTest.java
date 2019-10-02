package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.enr.EnrScheme;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * ENR serialization/deserialization test
 *
 * <p>ENR - Ethereum Node Record, according to <a
 * href="https://eips.ethereum.org/EIPS/eip-778">https://eips.ethereum.org/EIPS/eip-778</a>
 */
public class NodeRecordTest {
  private static final NodeRecordFactory NODE_RECORD_FACTORY = NodeRecordFactory.DEFAULT;

  @Test
  public void testLocalhostV4() throws Exception {
    final String expectedHost = "127.0.0.1";
    final Integer expectedUdpPort = 30303;
    final Integer expectedTcpPort = null;
    final UInt64 expectedSeqNumber = UInt64.valueOf(1);
    final BytesValue expectedPublicKey =
        BytesValue.fromHexString(
            "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138");
    final BytesValue expectedSignature =
        BytesValue.fromHexString(
            "7098ad865b00a582051940cb9cf36836572411a47278783077011599ed5cd16b76f2635f4e234738f30813a89eb9137e3e3df5266e3a1f11df72ecf1145ccb9c");

    final String localhostEnr =
        "-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8";
    NodeRecord nodeRecord = NODE_RECORD_FACTORY.fromBase64(localhostEnr);

    assertEquals(EnrScheme.V4, nodeRecord.getIdentityScheme());
    assertArrayEquals(
        InetAddress.getByName(expectedHost).getAddress(),
        ((BytesValue) nodeRecord.get(NodeRecord.FIELD_IP_V4)).extractArray());
    assertEquals(expectedUdpPort, nodeRecord.get(NodeRecord.FIELD_UDP_V4));
    assertEquals(expectedTcpPort, nodeRecord.get(NodeRecord.FIELD_TCP_V4));
    assertEquals(expectedSeqNumber, nodeRecord.getSeq());
    assertEquals(expectedPublicKey, nodeRecord.get(NodeRecord.FIELD_PKEY_SECP256K1));
    assertEquals(expectedSignature, nodeRecord.getSignature());

    String localhostEnrRestored = nodeRecord.asBase64();
    // The order of fields is not strict so we don't compare strings
    NodeRecord nodeRecordRestored = NODE_RECORD_FACTORY.fromBase64(localhostEnrRestored);

    assertEquals(EnrScheme.V4, nodeRecordRestored.getIdentityScheme());
    NodeRecord nodeRecordV4Restored = (NodeRecord) nodeRecordRestored;
    assertArrayEquals(
        InetAddress.getByName(expectedHost).getAddress(),
        ((BytesValue) nodeRecordV4Restored.get(NodeRecord.FIELD_IP_V4)).extractArray());
    assertEquals(expectedUdpPort, nodeRecordV4Restored.get(NodeRecord.FIELD_UDP_V4));
    assertEquals(expectedTcpPort, nodeRecordV4Restored.get(NodeRecord.FIELD_TCP_V4));
    assertEquals(expectedSeqNumber, nodeRecordV4Restored.getSeq());
    assertEquals(expectedPublicKey, nodeRecordV4Restored.get(NodeRecord.FIELD_PKEY_SECP256K1));
    assertEquals(expectedSignature, nodeRecordV4Restored.getSignature());
  }
}
