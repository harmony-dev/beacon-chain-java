package org.ethereum.beacon.test.runner;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

import java.math.BigInteger;
import java.util.Optional;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.test.type.SszTestCase;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** TestRunner for {@link SszTestCase} */
public class SszRunner implements Runner {
  private SszTestCase testCase;
  private BeaconChainSpec spec;
  private SSZSchemeBuilder.SSZScheme currentScheme;
  private SSZSerializer sszSerializer;

  public SszRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszTestCase)) {
      throw new RuntimeException("TestCase runner accepts only SszTestCase.class as input!");
    }
    this.testCase = (SszTestCase) testCase;
    this.spec = spec;
    SSZBuilder builder = new SSZBuilder();
    builder.withSSZSchemeBuilder(clazz -> currentScheme);
    this.sszSerializer = builder.buildSerializer();
  }

  private void activateSchemeMock(String type) {
    if (!type.startsWith("uint")) {
      throw new RuntimeException("Type " + testCase.getType() + " is not supported");
    }

    this.currentScheme = new SSZSchemeBuilder.SSZScheme();
    SSZField field = new SSZField(
        BigInteger.class,
        null,
        "uint",
        Integer.valueOf(testCase.getType().substring(4)),
        "value",
        "getValue");

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
