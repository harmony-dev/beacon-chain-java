package org.ethereum.beacon.test.runner.bls;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.bls.BlsPrivateToPublicCase;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsPrivateToPublicCase}
 *
 * <p>Verifies public key creation using private key
 */
public class BlsPrivateToPublic implements Runner {
  private BlsPrivateToPublicCase testCase;
  private BeaconChainSpec spec;

  public BlsPrivateToPublic(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsPrivateToPublicCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsPrivateToPublicCase as input!");
    }
    this.testCase = (BlsPrivateToPublicCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {
    BLS381.KeyPair keyPair =
        BLS381.KeyPair.create(BLS381.PrivateKey.create(Bytes32.fromHexString(testCase.getInput())));
    return assertHexStrings(testCase.getOutput(), keyPair.getPublic().getEncodedBytes().toString());
  }
}
