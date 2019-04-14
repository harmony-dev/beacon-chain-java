package org.ethereum.beacon.test.runner.hash;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.ssz.DefaultSSZ;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.type.SimpleTypeResolver;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.hash.TreeHashSimpleTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Function;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/** TestRunner for {@link TreeHashSimpleTestCase} */
public class TreeHashSimpleRunner implements Runner {
  private TreeHashSimpleTestCase testCase;
  private BeaconChainSpec spec;
  private SSZObjectHasher hasher;
  private SSZField field;

  public TreeHashSimpleRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof TreeHashSimpleTestCase)) {
      throw new RuntimeException("TestCase runner accepts only TreeHashSimpleTestCase.class as input!");
    }
    this.testCase = (TreeHashSimpleTestCase) testCase;
    this.spec = spec;
    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    SSZHasher sszHasher = DefaultSSZ.createCommonSSZBuilder(spec.getConstants())
        .withIncrementalHasher(true).
            withTypeResolverBuilder(objects -> {
              TypeResolver delegate = new SimpleTypeResolver(objects.getValue0(), objects.getValue1());
              return descriptor -> delegate.resolveSSZType(field);
            })
            .buildHasher(hashFunction);

    this.hasher = new SSZObjectHasher(sszHasher);
  }

  private void activateSchemeMock(String type) {
    if (type.equals("bool")) {
      this.field = new SSZField(Boolean.class, null, null, null, "value", "getValue");
    } else if (type.startsWith("uint")) {
      this.field =
          new SSZField(
              BigInteger.class,
              null,
              "uint",
              Integer.valueOf(type.substring(4)),
              "value",
              "getValue");
    } else {
      throw new RuntimeException("Type " + testCase.getType() + " is not supported");
    }
  }

  public Optional<String> run() {
    activateSchemeMock(testCase.getType());
    Hash32 res;
    if (testCase.getType().equals("bool")) {
      res = hasher.getHash(Boolean.valueOf(testCase.getValue()));
    } else if (testCase.getType().startsWith("uint")) {
      res = hasher.getHash(new BigInteger(testCase.getValue()));
    } else {
      throw new RuntimeException("Type " + testCase.getType() + " is not supported");
    }
    String expected = testCase.getRoot();
    if (!expected.startsWith("0x")) {
      expected = "0x" + expected;
    }
    String actual = res.toString();
    return assertEquals(expected, actual);
  }
}
