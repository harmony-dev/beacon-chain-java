package org.ethereum.beacon.test.runner.ssz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszGenericCase;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for Uints {@link SszGenericCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszUintsRunner implements Runner {
  private SszGenericCase testCase;
  private BeaconChainSpec spec;
  private ObjectMapper yamlMapper = new YAMLMapper();
  private SSZSchemeBuilder.SSZScheme currentScheme;
  private SSZSerializer sszSerializer;

  public SszUintsRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszGenericCase)) {
      throw new RuntimeException("TestCase runner accepts only SszGenericCase.class as input!");
    }
    if (!((SszGenericCase) testCase).getSubTypeName().startsWith("uint")) {
      throw new RuntimeException(
          "Type " + ((SszGenericCase) testCase).getSubTypeName() + " is not supported");
    }
    this.testCase = (SszGenericCase) testCase;
    this.spec = spec;
    SSZBuilder builder = new SSZBuilder();
    builder.withSSZSchemeBuilder(clazz -> currentScheme);
    this.sszSerializer = builder.buildSerializer();
  }

  private void activateSchemeMock(String type) {
    int startSize = type.indexOf('_');
    int endSize = type.indexOf('_', startSize + 1);
    String size = type.substring(startSize + 1, endSize);

    this.currentScheme = new SSZSchemeBuilder.SSZScheme();
    SSZField field =
        new SSZField(BigInteger.class, null, "uint", Integer.valueOf(size), "value", "getValue");

    currentScheme.getFields().add(field);
  }

  public Optional<String> run() {
    activateSchemeMock(testCase.getSubTypeName());

    if (testCase.isValid()) {
      BigInteger expected = null;
      try {
        expected = yamlMapper.readValue(testCase.getValue(), BigInteger.class);
      } catch (IOException e) {
        throw new RuntimeException("Unable to read expected value from file", e);
      }
      UIntTester actual = sszSerializer.decode(testCase.getSerialized(), UIntTester.class);
      return assertEquals(expected, actual.getValue());
    } else {
      try {
        UIntTester actual = sszSerializer.decode(testCase.getSerialized(), UIntTester.class);
      } catch (Exception ex) {
        return Optional.empty();
      }
      return Optional.of(
          "SSZ encoded data ["
              + testCase.getSerialized()
              + "] is not valid but was successfully decoded.");
    }
  }

  @SSZSerializable
  public static class UIntTester {
    private BigInteger value;

    public BigInteger getValue() {
      return value;
    }

    public void setValue(BigInteger value) {
      this.value = value;
    }
  }
}
