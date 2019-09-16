package org.ethereum.beacon.discovery;

import org.junit.Test;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;

import static org.junit.Assert.assertEquals;

public class RlpExtraTest {
  private static final String ABC = "abc";
  private static final int INT = 12345;

  /**
   * Testing what could we do when rlp is appended with some extra data, how could we split rlp and
   * that data
   */
  @Test
  public void testMakeRlpExtra() {
    RlpList rlpList = new RlpList(RlpString.create(ABC), RlpString.create(INT));
    byte[] encoded = RlpEncoder.encode(rlpList);
    byte[] encodedPlus = new byte[encoded.length + 2];
    System.arraycopy(encoded, 0, encodedPlus, 0, encoded.length);
    encodedPlus[encodedPlus.length - 2] = 0x12;
    encodedPlus[encodedPlus.length - 1] = 0x34;
    RlpList rlpList1 = RlpDecoder.decode(encodedPlus);
    RlpList rlpList2 = ((RlpList) rlpList1.getValues().get(0));
    assertEquals(ABC, new String(((RlpString) rlpList2.getValues().get(0)).getBytes()));
    assertEquals(INT, ((RlpString) rlpList2.getValues().get(1)).asPositiveBigInteger().intValue());
    // but what else could we do?
    int length = RlpEncoder.encode(rlpList2).length;
    assertEquals(length + 2, encodedPlus.length);
  }
}
