package org.ethereum.beacon.test.runner.ssz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszGenericCase;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitvector;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Optional;

import static org.ethereum.beacon.ssz.type.SSZType.VARIABLE_SIZE;
import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for Bitvectors {@link SszGenericCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszBitvectorRunner implements Runner {
  private SszGenericCase testCase;
  private BeaconChainSpec spec;
  private ObjectMapper yamlMapper = new YAMLMapper();
  private SSZSchemeBuilder.SSZScheme currentScheme;
  private SSZSerializer sszSerializer;

  public SszBitvectorRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszGenericCase)) {
      throw new RuntimeException("TestCase runner accepts only SszGenericCase.class as input!");
    }
    this.testCase = (SszGenericCase) testCase;
    if (!((SszGenericCase) testCase).getTypeName().startsWith("bitvec")) {
      throw new RuntimeException(
          "Type " + ((SszGenericCase) testCase).getTypeName() + " is not supported");
    }
    this.spec = spec;
    SSZBuilder builder = new SSZBuilder();
    builder.withSSZSchemeBuilder(clazz -> currentScheme);
    this.sszSerializer = builder.buildSerializer();
  }

  private void activateSchemeMock() {
    this.currentScheme = new SSZSchemeBuilder.SSZScheme();
    SSZField field =
        new SSZField(
            Bitvector.class,
            new SSZListMock(getVectorSize(), VARIABLE_SIZE),
            null,
            null,
            "value",
            "getValue");

    currentScheme.getFields().add(field);
  }

  private int getVectorSize() {
    String type = testCase.getSubTypeName();
    int startSize = type.indexOf('_');
    int endSize = type.indexOf('_', startSize + 1);
    if (endSize == -1) {
      endSize = type.length();
    }
    String size = type.substring(startSize + 1, endSize);
    return Integer.parseInt(size);
  }

  public Optional<String> run() {
    activateSchemeMock();
    if (testCase.isValid()) {
      Bitvector expected = null;
      try {
        String hexData = yamlMapper.readValue(testCase.getValue(), String.class);
        expected = Bitvector.of(getVectorSize(), BytesValue.fromHexString(hexData));
      } catch (IOException e) {
        throw new RuntimeException("Unable to read expected value from file", e);
      }
      BitvectorTester actual =
          sszSerializer.decode(testCase.getSerialized(), BitvectorTester.class);
      return assertEquals(expected, actual.getValue());
    } else {
      try {
        BitvectorTester actual =
            sszSerializer.decode(testCase.getSerialized(), BitvectorTester.class);
        System.out.println("ouch");
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
  public static class BitvectorTester {
    private Bitvector value;

    public Bitvector getValue() {
      return value;
    }

    public void setValue(Bitvector value) {
      this.value = value;
    }
  }

  private static class SSZListMock implements SSZ {
    private int vectorLength;
    private long maxSize;

    public SSZListMock(int vectorLength, long maxSize) {
      this.vectorLength = vectorLength;
      this.maxSize = maxSize;
    }

    @Override
    public java.lang.String type() {
      return null;
    }

    @Override
    public int vectorLength() {
      return vectorLength;
    }

    @Override
    public java.lang.String vectorLengthVar() {
      return "";
    }

    @Override
    public long maxSize() {
      return maxSize;
    }

    @Override
    public java.lang.String maxSizeVar() {
      return "";
    }

    @Override
    public int order() {
      return 0;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return SSZ.class;
    }
  }
}
