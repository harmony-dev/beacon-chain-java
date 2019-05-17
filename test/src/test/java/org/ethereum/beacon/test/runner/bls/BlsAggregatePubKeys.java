package org.ethereum.beacon.test.runner.bls;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.bls.BlsTest;
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
  private BeaconChainSpec spec;

  public BlsAggregatePubKeys(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsTest.BlsAggregatePubKeysCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsAggregatePubKeysCase as input!");
    }
    this.testCase = (BlsTest.BlsAggregatePubKeysCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {
    List<BLSPubkey> pubkeys = new ArrayList<>();
    for (String sig : testCase.getInput()) {
      BLSPubkey blsPubkey = BLSPubkey.wrap(Bytes48.fromHexString(sig));
      pubkeys.add(blsPubkey);
    }

    return assertHexStrings(
        testCase.getOutput(),
        spec.bls_aggregate_pubkeys(pubkeys).getEncodedBytes().toString());
  }
}
