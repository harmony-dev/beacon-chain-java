package org.ethereum.beacon.discovery;

import org.javatuples.Triplet;
import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.junit.Assert.assertEquals;

public class FunctionsTest {
  @Test
  public void testLogDistance() {
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
    assertEquals(0, Functions.logDistance(nodeId1a, nodeId1a));
    assertEquals(1, Functions.logDistance(nodeId0, nodeId1a));
    // So it's big endian
    assertEquals(253, Functions.logDistance(nodeId0, nodeId1b));
    assertEquals(253, Functions.logDistance(nodeId0, nodeId1s));
    assertEquals(256, Functions.logDistance(nodeId0, nodeId9s));
    // maximum distance
    assertEquals(256, Functions.logDistance(nodeId0, nodeIdfs));
    // logDistance is not an additive function
    assertEquals(255, Functions.logDistance(nodeId9s, nodeIdfs));
  }

  @Test
  public void hkdfExpandTest() {
    BytesValue testKeyA =
        BytesValue.fromHexString(
            "eef77acb6c6a6eebc5b363a475ac583ec7eccdb42b6481424c60f59aa326547f");
    BytesValue testKeyB =
        BytesValue.fromHexString(
            "66fb62bfbd66b9177a138c1e5cddbe4f7c30c343e94e68df8769459cb1cde628");
    BytesValue idNonce = Bytes32.ZERO;
    byte[] homeNodeIdBytes = new byte[32];
    homeNodeIdBytes[0] = 0x01;
    byte[] destNodeIdBytes = new byte[32];
    destNodeIdBytes[0] = 0x02;
    BytesValue homeNodeId = BytesValue.wrap(homeNodeIdBytes);
    BytesValue destNodeId = BytesValue.wrap(destNodeIdBytes);
    Triplet<BytesValue, BytesValue, BytesValue> sec1 =
        Functions.hkdf_expand(
            homeNodeId,
            destNodeId,
            testKeyA,
            BytesValue.wrap(ECKeyPair.create(testKeyB.extractArray()).getPublicKey().toByteArray()),
            idNonce);
    Triplet<BytesValue, BytesValue, BytesValue> sec2 =
        Functions.hkdf_expand(
            homeNodeId,
            destNodeId,
            testKeyB,
            BytesValue.wrap(ECKeyPair.create(testKeyA.extractArray()).getPublicKey().toByteArray()),
            idNonce);
    assertEquals(sec1, sec2);
  }
}
