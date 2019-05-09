package org.ethereum.beacon.test.runner.hash;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.SSZBuilder;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertTrue;
import static org.ethereum.beacon.test.runner.hash.TreeHashBasicRunner.instantiateValue;
import static org.ethereum.beacon.test.runner.hash.TreeHashBasicRunner.parseBasicFieldType;

/** TestRunner for {@link TreeHashListTestCase} for vector cases */
public class TreeHashVectorRunner implements Runner {
  private TreeHashListTestCase testCase;
  private SSZObjectHasher hasher;
  private SSZField field;
  private int vectorSize;

  public TreeHashVectorRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof TreeHashListTestCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only TreeHashListTestCase.class as input!");
    }
    this.testCase = (TreeHashListTestCase) testCase;
    Function<BytesValue, Hash32> hashFunction = Hashes::sha256;
    SSZHasher sszHasher =
        new SSZBuilder().withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
            .withIncrementalHasher(true)
            .withTypeResolverBuilder(
                objects -> {
                  return new CustomTypeResolver(objects.getValue0(), objects.getValue1());
                })
            .buildHasher(hashFunction);
    this.hasher = new SSZObjectHasher(sszHasher);
  }

  private void activateSchemeMock(String type, int vectorSize) {
    this.vectorSize = vectorSize;
    this.field = parseBasicFieldType(type);
  }

  public Optional<String> run() {
    assertTrue("Only two type parameters vectors are supported", testCase.getType().size() == 2);
    String internalType = testCase.getType().get(0);
    String vectorSize = testCase.getType().get(1);
    activateSchemeMock(internalType, Integer.valueOf(vectorSize));
    List input =
        testCase.getValue().stream()
            .map(v -> instantiateValue(v, internalType))
            .collect(Collectors.toList());
    Hash32 res = hasher.getHash(input);
    String expected = testCase.getRoot();
    if (!expected.startsWith("0x")) {
      expected = "0x" + expected;
    }
    String actual = res.toString();
    return assertEquals(expected, actual);
  }

  class CustomTypeResolver extends SimpleTypeResolver {
    private AccessorResolver accessorResolver;

    public CustomTypeResolver(
        AccessorResolver accessorResolver, ExternalVarResolver externalVarResolver) {
      super(accessorResolver, externalVarResolver);
      this.accessorResolver = accessorResolver;
    }

    @Override
    public SSZType resolveSSZType(SSZField descriptor) {
      if (new HashSet(Arrays.asList(descriptor.getRawClass().getInterfaces()))
          .contains(List.class)) {
        return new SSZListType(
            descriptor, this, accessorResolver.resolveListAccessor(descriptor).get(), vectorSize);
      } else {
        return super.resolveSSZType(field);
      }
    }
  }
}
