package org.ethereum.beacon.test.runner.hash;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.access.AccessorResolver;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.SimpleTypeResolver;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.hash.TreeHashContainerTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.runner.hash.TreeHashBasicRunner.instantiateValue;
import static org.ethereum.beacon.test.runner.hash.TreeHashBasicRunner.parseBasicFieldType;

/** TestRunner for {@link TreeHashContainerTestCase} */
public class TreeHashContainerRunner implements Runner {
  private TreeHashContainerTestCase testCase;
  private SSZObjectHasher hasher;
  private List<CustomTypeValue> fields;

  public TreeHashContainerRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof TreeHashContainerTestCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only TreeHashContainerTestCase.class as input!");
    }
    this.testCase = (TreeHashContainerTestCase) testCase;
    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
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

  public Optional<String> run() {
    // Guaranteeing order when matching type and value content maps
    List<CustomTypeValue> entries = new ArrayList<>();
    Map<String, CustomTypeValue> entrySet = new HashMap<>();
    testCase
        .getType()
        .forEach(
            (key, value) -> {
              CustomTypeValue entry = new CustomTypeValue();
              entry.setType(value);
              entrySet.put(key, entry);
            });
    testCase
        .getValue()
        .forEach(
            (key, value) -> {
              CustomTypeValue entry = entrySet.get(key);
              entry.setValue(value);
            });
    entrySet.entrySet().stream()
        .sorted(
            Comparator.comparingInt(
                (Map.Entry<String, CustomTypeValue> entry) -> {
                  return Integer.valueOf(entry.getKey().substring(5)); // "fieldXXX"
                }))
        .forEachOrdered(e -> entries.add(e.getValue()));
    this.fields = entries;

    String expected = testCase.getRoot();
    if (!expected.startsWith("0x")) {
      expected = "0x" + expected;
    }
    String actual = hasher.getHash(entries).toString();
    return assertEquals(expected, actual);
  }

  static class TestContainerAccessor implements SSZContainerAccessor {
    private List<CustomTypeValue> fields;

    public TestContainerAccessor(List<CustomTypeValue> fields) {
      this.fields = fields;
    }

    @Override
    public ContainerAccessor getAccessor(SSZField containerDescriptor) {
      return new ContainerAccessor() {
        @Override
        public List<SSZField> getChildDescriptors() {
          return fields.stream()
              .map(
                  field -> {
                    String type = field.getType();
                    return parseBasicFieldType(type);
                  })
              .collect(Collectors.toList());
        }

        @Override
        public Object getChildValue(Object compositeInstance, int childIndex) {
          String value = fields.get(childIndex).getValue();
          String type = fields.get(childIndex).getType();
          return instantiateValue(value, type);
        }
      };
    }

    @Override
    public ContainerInstanceBuilder createInstanceBuilder(SSZField containerDescriptor) {
      return null;
    }

    @Override
    public boolean isSupported(SSZField field) {
      return field.getRawClass().equals(SerializableContainerMock.class);
    }
  }

  static class CustomTypeValue {
    private String type;
    private String value;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @SSZSerializable
  public static class SerializableContainerMock {}

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
        TestContainerAccessor containerAccessor = new TestContainerAccessor(fields);
        return new SSZContainerType(this, descriptor, containerAccessor);
      } else {
        return super.resolveSSZType(descriptor);
      }
    }
  }
}
