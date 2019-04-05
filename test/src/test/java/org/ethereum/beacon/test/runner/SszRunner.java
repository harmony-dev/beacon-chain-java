package org.ethereum.beacon.test.runner;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.test.type.SszTestCase;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.Optional;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/** TestRunner for {@link SszTestCase} */
public class SszRunner implements Runner {
  private SszTestCase testCase;
  private SpecHelpers specHelpers;
  private SSZSchemeBuilder.SSZScheme currentScheme;
  private SSZSerializer sszSerializer;

  public SszRunner(TestCase testCase, SpecHelpers specHelpers) {
    if (!(testCase instanceof SszTestCase)) {
      throw new RuntimeException("TestCase runner accepts only SszTestCase.class as input!");
    }
    this.testCase = (SszTestCase) testCase;
    this.specHelpers = specHelpers;
//    SSZSerializerBuilder builder = new SSZSerializerBuilder();
//    builder.withSSZCodecResolver(new SSZCodecRoulette());
//    builder.withSSZModelFactory(
//        new SSZModelCreator()
//            .registerObjCreator(new ConstructorObjCreator())
//            .registerObjCreator(new SettersObjCreator()));
//    builder.withSSZSchemeBuilder(clazz -> currentScheme);
//    builder.addCodec(new UIntPrimitive());
//    builder.addCodec(new BytesPrimitive());
//    builder.addCodec(new BooleanPrimitive());
//    builder.addCodec(new StringPrimitive());
//    this.sszSerializer = builder.build();
  }

  private void activateSchemeMock(String type) {
    if (!type.startsWith("uint")) {
      throw new RuntimeException("Type " + testCase.getType() + " is not supported");
    }

    this.currentScheme = new SSZSchemeBuilder.SSZScheme();
    SSZSchemeBuilder.SSZScheme.SSZField field = new SSZSchemeBuilder.SSZScheme.SSZField();
    field.name = "value";
    field.fieldType = BigInteger.class;
    field.getter = "getValue";
    field.extraSize = Integer.valueOf(testCase.getType().substring(4));
    field.extraType = "uint";

    currentScheme.getFields().add(field);
  }

  public Optional<String> run() {
    activateSchemeMock(testCase.getType());

    if (Boolean.valueOf(testCase.getValid())) {
      byte[] data = BytesValue.fromHexString(testCase.getSsz()).getArrayUnsafe();
      BigInteger expected = new BigInteger(testCase.getValue());
      UIntTester actual = sszSerializer.decode(data, UIntTester.class);
      return assertEquals(expected, actual.getValue());
    } else {
      try {
        byte[] data = BytesValue.fromHexString(testCase.getSsz()).getArrayUnsafe();
        UIntTester actual = sszSerializer.decode(data, UIntTester.class);
      } catch (Exception ex) {
        return Optional.empty();
      }
      return Optional.of(
          "SSZ encoded data ["
              + testCase.getSsz()
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
