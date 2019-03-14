package org.ethereum.beacon.test.runner;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.bls.codec.PointData;
import org.ethereum.beacon.crypto.bls.milagro.BIGs;
import org.ethereum.beacon.crypto.bls.milagro.FPs;
import org.ethereum.beacon.crypto.bls.milagro.MilagroMessageMapper;
import org.ethereum.beacon.test.type.BlsTest;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * TestRunner for {@link BlsTest.BlsMessageHashCompressedCase}
 *
 * <p>Hash message
 * Input:
 * - Message as bytes
 * - domain as uint64
 * Output:
 * - Message hash as a compressed G2 point
 */
public class BlsMessageHashCompressed implements Runner {
  private BlsTest.BlsMessageHashCompressedCase testCase;
  private SpecHelpers specHelpers;

  public BlsMessageHashCompressed(TestCase testCase, SpecHelpers specHelpers) {
    if (!(testCase instanceof BlsTest.BlsMessageHashCompressedCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only BlsMessageHashCompressedCase as input!");
    }
    this.testCase = (BlsTest.BlsMessageHashCompressedCase) testCase;
    this.specHelpers = specHelpers;
  }

  public Optional<String> run() {

    MessageParameters messageParameters =
        MessageParameters.create(
            Hash32.wrap(Bytes32.fromHexString(testCase.getInput().getMessage())),
            Bytes8.fromHexStringLenient(testCase.getInput().getDomain()));
    MilagroMessageMapper milagroMessageMapper = new MilagroMessageMapper();
    ECP2 point = milagroMessageMapper.map(messageParameters);
    byte[] re = BIGs.toByteArray(point.getX().getA());
    byte[] im = BIGs.toByteArray(point.getX().getB());
    int sign = FPs.getSign(point.getY().getB());
    PointData.G2 data = PointData.G2.create(im, re, point.is_infinity(), sign);

    Optional<String> compareX1 =
        assertHexStrings(testCase.getOutput().get(0), BytesValue.wrap(data.getX1()).toString());
    Optional<String> compareX2 =
        assertHexStrings(testCase.getOutput().get(1), BytesValue.wrap(data.getX2()).toString());

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
