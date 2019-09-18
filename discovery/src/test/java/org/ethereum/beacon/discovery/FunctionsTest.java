package org.ethereum.beacon.discovery;

import org.junit.Test;
import tech.pegasys.artemis.util.bytes.Bytes32;

import static org.junit.Assert.assertEquals;

public class FunctionsTest {
  @Test
  public void testLogDistance() {
    Bytes32 nodeId0 = Bytes32.fromHexString("0000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 nodeId1a = Bytes32.fromHexString("0000000000000000000000000000000000000000000000000000000000000001");
    Bytes32 nodeId1b = Bytes32.fromHexString("1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 nodeId1s = Bytes32.fromHexString("1111111111111111111111111111111111111111111111111111111111111111");
    Bytes32 nodeId9s = Bytes32.fromHexString("9999999999999999999999999999999999999999999999999999999999999999");
    Bytes32 nodeIdfs = Bytes32.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    assertEquals(0, Functions.logDistance(nodeId0, nodeId1a));
    // So it's big endian
    assertEquals(252, Functions.logDistance(nodeId0, nodeId1b));
    assertEquals(252, Functions.logDistance(nodeId0, nodeId1s));
    assertEquals(255, Functions.logDistance(nodeId0, nodeId9s));
    // maximum distance
    assertEquals(256, Functions.logDistance(nodeId0, nodeIdfs));
    // logDistance is not an additive function
    assertEquals(255, Functions.logDistance(nodeId1s, nodeIdfs));
  }
}
