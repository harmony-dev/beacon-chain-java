package org.ethereum.beacon.test.runner.bls;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.bls.milagro.MilagroCodecs;
import org.ethereum.beacon.crypto.bls.milagro.MilagroMessageMapper;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.bls.BlsMessageHashCompressedCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;

import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsMessageHashCompressedCase}
 *
 * <p>Hash message Input: - Message as bytes - domain as uint64 Output: - Message hash as a
 * compressed G2 point
 */
public class BlsMessageHashCompressed implements Runner {
  private BlsMessageHashCompressedCase testCase;
  private BeaconChainSpec spec;

  public BlsMessageHashCompressed(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof BlsMessageHashCompressedCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only BlsMessageHashCompressedCase as input!");
    }
    this.testCase = (BlsMessageHashCompressedCase) testCase;
    this.spec = spec;
  }

  public Optional<String> run() {

    MessageParameters messageParameters =
        MessageParameters.create(
            Hash32.wrap(Bytes32.fromHexString(testCase.getInput().getMessage())),
            Bytes8.fromHexStringLenient(testCase.getInput().getDomain()));
    MilagroMessageMapper milagroMessageMapper = new MilagroMessageMapper();
    ECP2 point = milagroMessageMapper.map(messageParameters);
    Bytes96 data = MilagroCodecs.G2.encode(point);

    Optional<String> compareX1 =
        assertHexStrings(testCase.getOutput().get(0), data.slice(0, Bytes48.SIZE).toString());
    Optional<String> compareX2 =
        assertHexStrings(testCase.getOutput().get(1), data.slice(Bytes48.SIZE).toString());

    if (!compareX1.isPresent() && !compareX2.isPresent()) {
      return Optional.empty();
    }
    StringBuilder errors = new StringBuilder();
    if (compareX1.isPresent()) {
      errors.append("X1 coordinate error:\n");
      errors.append(compareX1.get());
      errors.append("\n");
    }
    if (compareX2.isPresent()) {
      errors.append("X2 coordinate error:\n");
      errors.append(compareX2.get());
      errors.append("\n");
    }

    return Optional.of(errors.toString());
  }
}
