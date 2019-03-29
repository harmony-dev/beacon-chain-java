package org.ethereum.beacon.test.runner;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.test.type.BlsTest;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsTest.BlsPrivateToPublicCase}
 *
 * <p>Verifies public key creation using private key
 */
public class BlsPrivateToPublic implements Runner {
  private BlsTest.BlsPrivateToPublicCase testCase;
  private BeaconChainSpec spec;

  public BlsPrivateToPublic(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsTest.BlsPrivateToPublicCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsPrivateToPublicCase as input!");
    }
    this.testCase = (BlsTest.BlsPrivateToPublicCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {
    BLS381.KeyPair keyPair =
        BLS381.KeyPair.create(BLS381.PrivateKey.create(Bytes32.fromHexString(testCase.getInput())));
    return assertHexStrings(testCase.getOutput(), keyPair.getPublic().getEncodedBytes().toString());
  }
}
