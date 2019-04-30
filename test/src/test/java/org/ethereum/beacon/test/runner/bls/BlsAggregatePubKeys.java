package org.ethereum.beacon.test.runner.bls;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.bls.BlsAggregatePubKeysCase;
import tech.pegasys.artemis.util.bytes.Bytes48;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsAggregatePubKeysCase}
 *
 * <p>Aggregates public keys
 * Test format description: <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/aggregate_pubkeys.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/aggregate_pubkeys.md</a>
 */
public class BlsAggregatePubKeys implements Runner {
  private BlsAggregatePubKeysCase testCase;
  private BeaconChainSpec spec;

  public BlsAggregatePubKeys(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsAggregatePubKeysCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsAggregatePubKeysCase as input!");
    }
    this.testCase = (BlsAggregatePubKeysCase) testCase;
    assert spec.isBlsVerify();
    this.spec = spec;
  }

  public Optional<String> run() {
    List<BLSPubkey> pubkeys = new ArrayList<>();
    for (String sig : testCase.getInput()) {
      BLSPubkey blsPubkey = BLSPubkey.wrap(Bytes48.fromHexString(sig));
      pubkeys.add(blsPubkey);
    }

    return assertHexStrings(
        testCase.getOutput(), spec.bls_aggregate_pubkeys(pubkeys).getEncodedBytes().toString());
  }
}
