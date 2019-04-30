package org.ethereum.beacon.test.runner.bls;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.bls.milagro.MilagroCodecs;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.bls.BlsAggregateSigsCase;
import tech.pegasys.artemis.util.bytes.Bytes96;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsAggregateSigsCase}
 *
 * <p>Aggregates signatures
 * Test format description: <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/aggregate_sigs.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/bls/aggregate_sigs.md</a>
 */
public class BlsAggregateSigs implements Runner {
  private BlsAggregateSigsCase testCase;
  private BeaconChainSpec spec;

  public BlsAggregateSigs(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsAggregateSigsCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsAggregateSigsCase as input!");
    }
    this.testCase = (BlsAggregateSigsCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {
    List<BLSSignature> signatures = new ArrayList<>();
    for (String sig : testCase.getInput()) {
      BLSSignature blsSignature = BLSSignature.wrap(Bytes96.fromHexString(sig));
      signatures.add(blsSignature);
    }
    ECP2 product = new ECP2();
    signatures.forEach(
        (s) -> {
          product.add(MilagroCodecs.G2.decode(s));
        });

    return assertHexStrings(
        testCase.getOutput(), BLS381.Signature.create(product).getEncoded().toString());
  }
}
