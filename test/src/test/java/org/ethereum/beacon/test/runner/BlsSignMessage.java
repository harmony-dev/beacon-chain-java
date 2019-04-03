package org.ethereum.beacon.test.runner;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

import java.util.Optional;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.test.type.BlsTest;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;

/**
 * TestRunner for {@link BlsTest.BlsSignMessageCase}
 *
 * <p>Signs message by signer defined by private key
 */
public class BlsSignMessage implements Runner {
  private BlsTest.BlsSignMessageCase testCase;
  private BeaconChainSpec spec;

  public BlsSignMessage(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsTest.BlsSignMessageCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsSignMessageCase as input!");
    }
    this.testCase = (BlsTest.BlsSignMessageCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {
    BLS381.KeyPair keyPair =
        BLS381.KeyPair.create(
            BLS381.PrivateKey.create(Bytes32.fromHexString(testCase.getInput().getPrivkey())));

    MessageParameters messageParameters =
        MessageParameters.create(
            Hash32.wrap(Bytes32.fromHexString(testCase.getInput().getMessage())),
            Bytes8.fromHexStringLenient(testCase.getInput().getDomain()));
    BLSSignature signature =
        BLSSignature.wrap(BLS381.sign(messageParameters, keyPair).getEncoded());

    return assertEquals(
        BLSSignature.wrap(Bytes96.fromHexStringLenient(testCase.getOutput())), signature);
  }
}
