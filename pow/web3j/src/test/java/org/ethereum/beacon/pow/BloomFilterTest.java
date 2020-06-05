package org.ethereum.beacon.pow;

import org.ethereum.beacon.pow.util.BloomFilter;
import org.junit.Test;
import org.web3j.crypto.Hash;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.junit.Assert.assertTrue;

public class BloomFilterTest {
  /** Some real-life data */
  @Test
  public void test() {
    String contractAddress = "0x99bde68864f63fde1df78ffa1808c232de7ef61e";
    BytesValue contractDeployAddress = BytesValue.fromHexString(contractAddress);
    Hash32 contractDeployAddressHash =
        Hash32.wrap(Bytes32.wrap(Hash.sha3(contractDeployAddress.extractArray())));
    BloomFilter contractAddressBloom = BloomFilter.create(contractDeployAddressHash.extractArray());
    BloomFilter blockLogsBloom =
        new BloomFilter(
            BytesValue.fromHexString(
                    "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000400000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000008000000000000000000000000")
                .extractArray());
    assertTrue(blockLogsBloom.matches(contractAddressBloom));
  }
}
