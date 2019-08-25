package org.ethereum.beacon.test.runner.ssz;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.basic.UIntPrimitive;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.visitor.SSZReader;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszGenericCase;
import tech.pegasys.artemis.util.collections.ReadVector;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for vectors with basic values {@link SszGenericCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszBasicVectorRunner implements Runner {
  private SszGenericCase testCase;
  private BeaconChainSpec spec;
  private ObjectMapper yamlMapper = new YAMLMapper();
  private SSZSchemeBuilder.SSZScheme currentScheme;
  private SSZSerializer sszSerializer;
  private AtomicInteger currentIntSize = new AtomicInteger(0);

  public SszBasicVectorRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszGenericCase)) {
      throw new RuntimeException("TestCase runner accepts only SszGenericCase.class as input!");
    }
    if (!((SszGenericCase) testCase).getSubTypeName().startsWith("vec_")) {
      throw new RuntimeException(
          "Type " + ((SszGenericCase) testCase).getSubTypeName() + " is not supported");
    }
    this.testCase = (SszGenericCase) testCase;
    this.spec = spec;
    SSZBuilder builder = new SSZBuilder();
    builder.withSSZSchemeBuilder(clazz -> currentScheme);
    SSZBasicAccessor delegate = new UIntPrimitive();
    builder.addBasicCodecs(
        new SSZBasicAccessor() {
          @Override
          public Set<String> getSupportedSSZTypes() {
            return delegate.getSupportedSSZTypes();
          }

          @Override
          public Set<Class> getSupportedClasses() {
            return delegate.getSupportedClasses();
          }

          @Override
          public int getSize(SSZField field) {
            return currentIntSize.get() / 8;
          }

          @Override
          public void encode(Object value, SSZField field, OutputStream result) {
            delegate.encode(value, field, result);
          }

          @Override
          public Object decode(SSZField field, SSZReader reader) {
            SSZField updated =
                new SSZField(
                    field.getRawClass(),
                    field.getFieldAnnotation(),
                    field.getExtraType(),
                    currentIntSize.get(),
                    field.getName(),
                    field.getGetter());
            return delegate.decode(updated, reader);
          }
        });
    builder.addDefaultBasicCodecs();
    this.sszSerializer = builder.buildSerializer();
  }

  private void activateSchemeMock(String type) {
    if (type.startsWith("vec_uint")) {
      int intSizeStart = type.indexOf("_uint");
      int intSizeEnd = type.indexOf('_', intSizeStart + 1);
      String size = type.substring(intSizeStart + 5, intSizeEnd);
      int intSize = Integer.parseInt(size);
      currentIntSize.set(intSize);
      int vectorLengthStart = intSizeEnd + 1;
      int vectorLengthEnd = type.indexOf('_', vectorLengthStart);
      if (vectorLengthEnd == -1) {
        vectorLengthEnd = type.length();
      }
      String length = type.substring(vectorLengthStart, vectorLengthEnd);
      int vectorLength = Integer.parseInt(length);

      this.currentScheme = new SSZSchemeBuilder.SSZScheme();
      List<BigInteger> mockList = new ArrayList<>();
      IntStream.range(0, vectorLength).forEach(value -> mockList.add(BigInteger.ZERO));
      ReadVector<Integer, BigInteger> mock = ReadVector.wrap(mockList, Function.identity());
      SSZField field = SSZField.resolveFromValue(mock, ReadVector.class);
      SSZField updated =
          new SSZField(
              field.getParametrizedType(),
              field.getFieldAnnotation(),
              field.getExtraType(),
              field.getExtraSize(),
              "value",
              "getValue");
      currentScheme.getFields().add(updated);
    } else if (type.startsWith("vec_bool")) {
      int vectorLengthStart = type.indexOf("bool_");
      int vectorLengthEnd = type.indexOf('_', vectorLengthStart + 5);
      if (vectorLengthEnd == -1) {
        vectorLengthEnd = type.length();
      }
      String length = type.substring(vectorLengthStart + 5, vectorLengthEnd);
      int vectorLength = Integer.parseInt(length);

      this.currentScheme = new SSZSchemeBuilder.SSZScheme();
      List<Boolean> mockList = new ArrayList<>();
      IntStream.range(0, vectorLength).forEach(value -> mockList.add(false));
      ReadVector<Integer, Boolean> mock = ReadVector.wrap(mockList, Function.identity());
      SSZField field = SSZField.resolveFromValue(mock, ReadVector.class);
      SSZField updated =
          new SSZField(
              field.getParametrizedType(),
              field.getFieldAnnotation(),
              field.getExtraType(),
              field.getExtraSize(),
              "value",
              "getValue");
      currentScheme.getFields().add(updated);
    } else {
      throw new RuntimeException("Type " + type + " is not supported");
    }
  }

  public Optional<String> run() {
    activateSchemeMock(testCase.getSubTypeName());

    if (testCase.getSubTypeName().startsWith("vec_uint")) {
      if (testCase.isValid()) {
        List<BigInteger> expected = null;
        try {
          expected =
              yamlMapper.readValue(testCase.getValue(), new TypeReference<List<BigInteger>>() {});
        } catch (IOException e) {
          throw new RuntimeException("Unable to read expected value from file", e);
        }
        UIntVectorTester actual =
            sszSerializer.decode(testCase.getSerialized(), UIntVectorTester.class);
        return assertEquals(expected, actual.getValue().listCopy());
      } else {
        try {
          UIntVectorTester actual =
              sszSerializer.decode(testCase.getSerialized(), UIntVectorTester.class);
        } catch (Exception ex) {
          return Optional.empty();
        }
        return Optional.of(
            "SSZ encoded data ["
                + testCase.getSerialized()
                + "] is not valid but was successfully decoded.");
      }
    } else if (testCase.getSubTypeName().startsWith("vec_bool")) {
      if (testCase.isValid()) {
        List<Boolean> expected = null;
        try {
          expected =
              yamlMapper.readValue(testCase.getValue(), new TypeReference<List<Boolean>>() {});
        } catch (IOException e) {
          throw new RuntimeException("Unable to read expected value from file", e);
        }
        BoolVectorTester actual =
            sszSerializer.decode(testCase.getSerialized(), BoolVectorTester.class);
        return assertEquals(expected, actual.getValue().listCopy());
      } else {
        try {
          BoolVectorTester actual =
              sszSerializer.decode(testCase.getSerialized(), BoolVectorTester.class);
        } catch (Exception ex) {
          return Optional.empty();
        }
        return Optional.of(
            "SSZ encoded data ["
                + testCase.getSerialized()
                + "] is not valid but was successfully decoded.");
      }
    } else {
      throw new RuntimeException("Type " + testCase.getSubTypeName() + " is not supported");
    }
  }

  @SSZSerializable
  public static class UIntVectorTester {
    private ReadVector<Integer, BigInteger> value;

    public ReadVector<Integer, BigInteger> getValue() {
      return value;
    }

    public void setValue(ReadVector<Integer, BigInteger> value) {
      this.value = value;
    }

    public void setValue(List<BigInteger> value) {
      this.value = ReadVector.wrap(value, Function.identity());
    }
  }

  @SSZSerializable
  public static class BoolVectorTester {
    private ReadVector<Integer, Boolean> value;

    public ReadVector<Integer, Boolean> getValue() {
      return value;
    }

    public void setValue(ReadVector<Integer, Boolean> value) {
      this.value = value;
    }
  }
}
