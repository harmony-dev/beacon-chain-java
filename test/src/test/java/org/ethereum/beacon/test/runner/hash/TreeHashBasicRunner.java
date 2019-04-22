package org.ethereum.beacon.test.runner.hash;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.SimpleTypeResolver;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.hash.TreeHashBasicTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Function;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/** TestRunner for {@link TreeHashBasicTestCase} */
public class TreeHashBasicRunner implements Runner {
  private TreeHashBasicTestCase testCase;
  private SSZObjectHasher hasher;
  private SSZField field;

  public TreeHashBasicRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof TreeHashBasicTestCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only TreeHashBasicTestCase.class as input!");
    }
    this.testCase = (TreeHashBasicTestCase) testCase;
    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    SSZHasher sszHasher =
        new SSZBuilder().withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
            .withIncrementalHasher(true)
            .withTypeResolverBuilder(
                objects -> {
                  TypeResolver delegate =
                      new SimpleTypeResolver(objects.getValue0(), objects.getValue1());
                  return descriptor -> delegate.resolveSSZType(field);
                })
            .buildHasher(hashFunction);
    this.hasher = new SSZObjectHasher(sszHasher);
  }

  public static SSZField parseBasicFieldType(String type) {
    if (type.equals("bool")) {
      return new SSZField(Boolean.class, null, null, null, "value", "getValue");
    } else if (type.startsWith("uint")) {
      return new SSZField(
          BigInteger.class, null, "uint", Integer.valueOf(type.substring(4)), "value", "getValue");
    } else {
      throw new RuntimeException("Type " + type + " is not supported");
    }
  }

  public static Object instantiateValue(String value, String type) {
    Object res;
    if (type.equals("bool")) {
      res = Boolean.valueOf(value);
    } else if (type.startsWith("uint")) {
      res = new BigInteger(value);
    } else {
      throw new RuntimeException("Type " + type + " is not supported");
    }

    return res;
  }

  private void activateSchemeMock(String type) {
    this.field = parseBasicFieldType(type);
  }

  public Optional<String> run() {
    activateSchemeMock(testCase.getType());
    Object object = instantiateValue(testCase.getValue(), testCase.getType());
    Hash32 res = hasher.getHash(object);
    String expected = testCase.getRoot();
    if (!expected.startsWith("0x")) {
      expected = "0x" + expected;
    }
    String actual = res.toString();
    return assertEquals(expected, actual);
  }
}
