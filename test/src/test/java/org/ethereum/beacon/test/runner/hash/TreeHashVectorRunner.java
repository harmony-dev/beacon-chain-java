package org.ethereum.beacon.test.runner.hash;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.ssz.DefaultSSZ;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.access.AccessorResolver;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.SimpleTypeResolver;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.hash.TreeHashListTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertTrue;

/** TestRunner for {@link TreeHashListTestCase} */
public class TreeHashVectorRunner implements Runner {
  private TreeHashListTestCase testCase;
  private BeaconChainSpec spec;
  private SSZObjectHasher hasher;
  private SSZField field;
  private int vectorSize;

  public TreeHashVectorRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof TreeHashListTestCase)) {
      throw new RuntimeException("TestCase runner accepts only TreeHashListTestCase.class as input!");
    }
    this.testCase = (TreeHashListTestCase) testCase;
    this.spec = spec;
    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    SSZHasher sszHasher =
        DefaultSSZ.createCommonSSZBuilder(spec.getConstants())
            .withIncrementalHasher(true)
            .withTypeResolverBuilder(
                objects -> {
                  return new CustomTypeResolver(objects.getValue0(), objects.getValue1());
                })
            .buildHasher(hashFunction);

    this.hasher = new SSZObjectHasher(sszHasher);
  }

  class CustomTypeResolver extends SimpleTypeResolver {
    private AccessorResolver accessorResolver;

    public CustomTypeResolver(AccessorResolver accessorResolver, ExternalVarResolver externalVarResolver) {
      super(accessorResolver, externalVarResolver);
      this.accessorResolver = accessorResolver;
    }

    @Override
    public SSZType resolveSSZType(SSZField descriptor) {
      if (new HashSet(Arrays.asList(descriptor.getRawClass().getInterfaces())).contains(List.class)) {
        return new SSZListType(descriptor, this, accessorResolver.resolveListAccessor(
            descriptor
        ).get(), vectorSize);
      } else {
        return super.resolveSSZType(field);
      }
    }
  }

  private void activateSchemeMock(String type, int vectorSize) {
    this.vectorSize = vectorSize;
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
    assertTrue("Only one type lists are supported", testCase.getType().size() == 2);
    String listType = testCase.getType().get(0);
    String vectorSize = testCase.getType().get(1);
    activateSchemeMock(listType, Integer.valueOf(vectorSize));
    Hash32 res;
    if (listType.equals("bool")) {
      List<Boolean> input = testCase.getValue().stream().map(Boolean::valueOf).collect(Collectors.toList());
      res = hasher.getHash(input);
    } else if (listType.startsWith("uint")) {
      List<BigInteger> input = testCase.getValue().stream().map(BigInteger::new).collect(Collectors.toList());
      res = hasher.getHash(input);
    } else {
      throw new RuntimeException("Type " + listType + " is not supported");
    }
    String expected = testCase.getRoot();
    if (!expected.startsWith("0x")) {
      expected = "0x" + expected;
    }
    String actual = res.toString();
    return assertEquals(expected, actual);
  }
}
