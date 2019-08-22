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
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for Boolean {@link SszGenericCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszBooleanRunner implements Runner {
  private SszGenericCase testCase;
  private BeaconChainSpec spec;
  private ObjectMapper yamlMapper = new YAMLMapper();
  private SSZSchemeBuilder.SSZScheme currentScheme;
  private SSZSerializer sszSerializer;

  public SszBooleanRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszGenericCase)) {
      throw new RuntimeException("TestCase runner accepts only SszGenericCase.class as input!");
    }
    this.testCase = (SszGenericCase) testCase;
    this.spec = spec;
    SSZBuilder builder = new SSZBuilder();
    builder.withSSZSchemeBuilder(clazz -> currentScheme);
    this.sszSerializer = builder.buildSerializer();
  }

  private void activateSchemeMock(String type) {
    if (!type.startsWith("bool")) {
      throw new RuntimeException("Type " + type + " is not supported");
    }
    this.currentScheme = new SSZSchemeBuilder.SSZScheme();
    SSZField field = new SSZField(Boolean.class, null, null, null, "value", "getValue");

    currentScheme.getFields().add(field);
  }

  public Optional<String> run() {
    activateSchemeMock(testCase.getTypeName());

    if (testCase.isValid()) {
      Boolean expected = null;
      try {
        expected = yamlMapper.readValue(testCase.getValue(), Boolean.class);
      } catch (IOException e) {
        throw new RuntimeException("Unable to read expected value from file", e);
      }
      BooleanTester actual = sszSerializer.decode(testCase.getSerialized(), BooleanTester.class);
      return assertEquals(expected, actual.getValue());
    } else {
      try {
        BooleanTester actual = sszSerializer.decode(testCase.getSerialized(), BooleanTester.class);
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
  public static class BooleanTester {
    private Boolean value;

    public Boolean getValue() {
      return value;
    }

    public void setValue(Boolean value) {
      this.value = value;
    }
  }
}
