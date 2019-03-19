package org.ethereum.beacon.test.runner;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.bls.milagro.MilagroCodecs;
import org.ethereum.beacon.test.type.BlsTest;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsTest.BlsAggregateSigsCase}
 *
 * <p>Aggregates signatures
 */
public class BlsAggregateSigs implements Runner {
  private BlsTest.BlsAggregateSigsCase testCase;
  private SpecHelpers specHelpers;

  public BlsAggregateSigs(TestCase testCase, SpecHelpers specHelpers) {
    if (!(testCase instanceof BlsTest.BlsAggregateSigsCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsAggregateSigsCase as input!");
    }
    this.testCase = (BlsTest.BlsAggregateSigsCase) testCase;
    this.specHelpers = specHelpers;
  }

  public Optional<String> run() {
    List<BLSSignature> signatures = new ArrayList<>();
    for (String sig : testCase.getInput()) {
      BLSSignature blsSignature = BLSSignature.wrap(Bytes96.fromHexString(sig));
      signatures.add(blsSignature);
    }
    ECP2 product = new ECP2();
    signatures.forEach((s) -> {product.add(MilagroCodecs.G2.decode(s));});

    return assertHexStrings(testCase.getOutput(), BLS381.Signature.create(product).getEncoded().toString());
  }
}
