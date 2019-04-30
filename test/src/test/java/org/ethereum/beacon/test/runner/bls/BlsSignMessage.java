package org.ethereum.beacon.test.runner.bls;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.bls.BlsSignMessageCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;

import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for {@link BlsSignMessageCase}
 *
 * <p>Signs message by signer defined by private key
 * Test format description: <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/sign_msg.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/sign_msg.md</a>
 */
public class BlsSignMessage implements Runner {
  private BlsSignMessageCase testCase;
  private BeaconChainSpec spec;

  public BlsSignMessage(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsSignMessageCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsSignMessageCase as input!");
    }
    this.testCase = (BlsSignMessageCase) testCase;
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
