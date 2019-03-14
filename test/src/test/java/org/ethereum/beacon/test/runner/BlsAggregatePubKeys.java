package org.ethereum.beacon.test.runner;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.test.type.BlsTest;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.util.bytes.Bytes48;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsTest.BlsAggregatePubKeysCase}
 *
 * <p>Aggregates public keys
 */
public class BlsAggregatePubKeys implements Runner {
  private BlsTest.BlsAggregatePubKeysCase testCase;
  private SpecHelpers specHelpers;

  public BlsAggregatePubKeys(TestCase testCase, SpecHelpers specHelpers) {
    if (!(testCase instanceof BlsTest.BlsAggregatePubKeysCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsAggregatePubKeysCase as input!");
    }
    this.testCase = (BlsTest.BlsAggregatePubKeysCase) testCase;
    this.specHelpers = specHelpers;
  }

  public Optional<String> run() {
    List<BLSPubkey> pubkeys = new ArrayList<>();
    for (String sig : testCase.getInput()) {
      BLSPubkey blsPubkey = BLSPubkey.wrap(Bytes48.fromHexString(sig));
      pubkeys.add(blsPubkey);
    }

    return assertHexStrings(
        testCase.getOutput(),
        specHelpers.bls_aggregate_pubkeys(pubkeys).getEncodedBytes().toString());
  }
}
