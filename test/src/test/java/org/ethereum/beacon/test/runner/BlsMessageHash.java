package org.ethereum.beacon.test.runner;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.apache.milagro.amcl.BLS381.FP2;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.bls.milagro.MilagroMessageMapper;
import org.ethereum.beacon.test.type.BlsTest;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertLists;

/**
 * TestRunner for {@link org.ethereum.beacon.test.type.BlsTest.BlsMessageHashCase}
 *
 * <p>Hash message
 * Input:
 * - Message as bytes
 * - domain as uint64
 * Output:
 * - Message hash as a G2 point (uncompressed)
 */
public class BlsMessageHash implements Runner {
  private BlsTest.BlsMessageHashCase testCase;
  private SpecHelpers specHelpers;

  public BlsMessageHash(TestCase testCase, SpecHelpers specHelpers) {
    if (!(testCase instanceof BlsTest.BlsMessageHashCase)) {
      throw new RuntimeException("TestCase runner accepts only BlsMessageHashCase as input!");
    }
    this.testCase = (BlsTest.BlsMessageHashCase) testCase;
    this.specHelpers = specHelpers;
  }

  public Optional<String> run() {

    MessageParameters messageParameters =
        MessageParameters.create(
            Hash32.wrap(Bytes32.fromHexString(testCase.getInput().getMessage())),
            Bytes8.fromHexStringLenient(testCase.getInput().getDomain()));
    MilagroMessageMapper milagroMessageMapper = new MilagroMessageMapper();
    ECP2 point = milagroMessageMapper.map(messageParameters);

    Optional<String> compareX =
        assertLists(testCase.getOutput().get(0), serialize(point.getX()));
    Optional<String> compareY =
        assertLists(testCase.getOutput().get(1), serialize(point.getY()));
    Optional<String> compareZ =
        assertLists(testCase.getOutput().get(2), serialize(point.getz()));

    if (!compareX.isPresent() && !compareY.isPresent() && !compareZ.isPresent()) {
      return Optional.empty();
    }
    StringBuilder errors = new StringBuilder();
    if (compareX.isPresent()) {
      errors.append("X coordinate error:\n");
      errors.append(compareX.get());
      errors.append("\n");
    }
    if (compareY.isPresent()) {
      errors.append("Y coordinate error:\n");
      errors.append(compareY.get());
      errors.append("\n");
    }
    if (compareZ.isPresent()) {
      errors.append("Z coordinate error:\n");
      errors.append(compareZ.get());
      errors.append("\n");
    }

    return Optional.of(errors.toString());
  }

  private List<String> serialize(FP2 coordinate) {
    List<String> res = new ArrayList<>();
    res.add("0x" + coordinate.getA().toString());
    res.add("0x" + coordinate.getB().toString());

    return res;
  }
}
