package org.ethereum.beacon.test.runner.hash;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.ssz.DefaultSSZ;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.ExternalVarResolver;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.access.AccessorResolver;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZListType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.SimpleTypeResolver;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.hash.TreeHashCompositeTestCase;
import org.ethereum.beacon.test.type.hash.TreeHashContainerTestCase;
import org.javatuples.Pair;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.runner.hash.TreeHashBasicRunner.instantiateValue;
import static org.ethereum.beacon.test.runner.hash.TreeHashBasicRunner.parseBasicFieldType;

/** TestRunner for {@link TreeHashContainerTestCase} */
public class TreeHashCompositeRunner implements Runner {
  List<Pair<SSZType, Object>> nodes = new ArrayList<>();
  private TreeHashCompositeTestCase testCase;
  private SSZObjectHasher hasher;
  private AccessorResolver accessorResolver;
  private TypeResolver typeResolver;
  private Integer nodeCounter = 0;
  private SSZType rootType;
  public TreeHashCompositeRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof TreeHashCompositeTestCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only TreeHashCompositeTestCase.class as input!");
    }
    this.testCase = (TreeHashCompositeTestCase) testCase;
    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    SSZHasher sszHasher =
        DefaultSSZ.createCommonSSZBuilder(spec.getConstants())
            .withIncrementalHasher(true)
            .withTypeResolverBuilder(
                objects -> {
                  this.accessorResolver = objects.getValue0();
                  this.typeResolver =
                      new CustomTypeResolver(objects.getValue0(), objects.getValue1());
                  return typeResolver;
                })
            .buildHasher(hashFunction);
    this.hasher = new SSZObjectHasher(sszHasher);
  }

  private SSZType parseCurrentType(Object typeObj, Object valueObj) {
    String currentName = String.valueOf(nodeCounter);
    SSZType result;
    if (new HashSet(Arrays.asList(typeObj.getClass().getInterfaces())).contains(List.class)) {
      List objList = (List) typeObj;
      List valueList = (List) valueObj;
      SSZField descriptor = new SSZField(ArrayList.class, null, null, null, currentName, null);
      TestListAccessor listAccessor =
          new TestListAccessor(() -> parseCurrentType(objList.get(0), valueObj), () -> valueList);
      if (objList.size() == 1) {
        this.nodeCounter++;
        result =
            new SSZCustomListType(
                descriptor,
                typeResolver,
                listAccessor,
                -1,
                () -> parseCurrentType(objList.get(0), valueObj));
        nodes.add(Pair.with(result, valueObj));
        return result;
      } else if (objList.size() == 2) {
        this.nodeCounter++;
        result =
            new SSZCustomListType(
                descriptor,
                typeResolver,
                listAccessor,
                (Integer) objList.get(1),
                () -> parseCurrentType(objList.get(0), valueObj));
        nodes.add(Pair.with(result, valueObj));
        return result;
      } else {
        throw new RuntimeException("Cannot parse " + typeObj + " type");
      }
    } else if (new HashSet(Arrays.asList(typeObj.getClass().getInterfaces())).contains(Map.class)) {
      Map typeMap = (Map) typeObj;
      Map valueMap = (Map) valueObj;
      SSZField descriptor =
          new SSZField(
              TreeHashContainerRunner.SerializableContainerMock.class,
              null,
              null,
              null,
              currentName,
              null);

      Supplier<List<SSZType>> childTypeSupplier =
          () -> {
            List<Map.Entry<String, String>> entries =
                (List<Map.Entry<String, String>>)
                    typeMap.entrySet().stream()
                        .sorted(
                            Comparator.comparingInt(
                                (Map.Entry<String, String> entry) -> {
                                  return Integer.valueOf(entry.getKey().substring(5)); // "fieldXXX"
                                }))
                        .collect(Collectors.toList());
            return entries.stream()
                .map(entry -> parseCurrentType(entry.getValue(), valueMap.get(entry.getKey())))
                .collect(Collectors.toList());
          };
      SSZContainerAccessor containerAccessor =
          new TestContainerAccessor(
              childTypeSupplier,
              () -> {
                List<Map.Entry<String, String>> entries =
                    (List<Map.Entry<String, String>>)
                        valueMap.entrySet().stream()
                            .sorted(
                                Comparator.comparingInt(
                                    (Map.Entry<String, String> entry) -> {
                                      return Integer.valueOf(
                                          entry.getKey().substring(5)); // "fieldXXX"
                                    }))
                            .collect(Collectors.toList());
                return entries.stream()
                    .map(
                        entry -> {
                          Object type = typeMap.get(entry.getKey());
                          if (type instanceof String) {
                            return instantiateValue(
                                entry.getValue(), (String) typeMap.get(entry.getKey()));
                          } else {
                            return null;
                          }
                        })
                    .collect(Collectors.toList());
              });
      SSZCustomContainerType customContainerType =
          new SSZCustomContainerType(
              typeResolver, descriptor, containerAccessor, childTypeSupplier);
      this.nodeCounter++;
      result = customContainerType;
      nodes.add(Pair.with(result, valueObj));
      return result;
    } else if (typeObj instanceof String) {
      this.nodeCounter++;
      SSZField basic = parseBasicFieldType((String) typeObj);
      SSZField input =
          new SSZField(
              basic.getRawClass(),
              basic.getFieldAnnotation(),
              basic.getExtraType(),
              basic.getExtraSize(),
              currentName,
              null);
      result = typeResolver.resolveSSZType(input);
      nodes.add(Pair.with(result, valueObj));
      return result;
    } else {
      throw new RuntimeException("Cannot parse " + typeObj + " type");
    }
  }

  public Optional<String> run() {
    this.nodeCounter = 0;
    this.nodes = new ArrayList<>();
    this.rootType = parseCurrentType(testCase.getType(), testCase.getValue());
    System.out.println(rootType.dumpHierarchy());
    // TODO: instead we should move along value
//    this.rootType = parseCurrentValue(testCase.getValue(), testCase.getType());

    String expected = testCase.getRoot();
    if (!expected.startsWith("0x")) {
      expected = "0x" + expected;
    }
    String actual = hasher.getHash(new MainMock()).toString();
    return assertEquals(expected, actual);
  }

  static class SSZCustomListType extends SSZListType {
    Supplier<SSZType> elementTypeSupplier;

    public SSZCustomListType(
        SSZField descriptor,
        TypeResolver typeResolver,
        SSZListAccessor accessor,
        int vectorLength,
        Supplier<SSZType> elementTypeSupplier) {
      super(descriptor, typeResolver, accessor, vectorLength);
      this.elementTypeSupplier = elementTypeSupplier;
    }

    @Override
    public SSZType getElementType() {
      return elementTypeSupplier.get();
    }
  }
  //
  //  private Pair<Object, SSZType> parseCurrentValue(Object valueObj, Object typeObj) {
  //    String currentName = String.valueOf(nodeCounter);
  //    if (new HashSet(Arrays.asList(valueObj.getClass().getInterfaces())).contains(List.class)) {
  //      List objList = (List) typeObj;
  //      List valueList = (List) valueObj;
  //      SSZField descriptor = new SSZField(ArrayList.class, null, null, null, currentName, null);
  //      TestListAccessor listAccessor = new TestListAccessor(
  //          () -> parseCurrentValue(valueList.get(0), valueObj),
  //          () -> valueList
  //      );
  //      if (objList.size() == 1) {
  //        this.nodeCounter++;
  //        result = new SSZCustomListType(
  //            descriptor,
  //            typeResolver,
  //            listAccessor,
  //            -1,
  //            () -> parseCurrentType(objList.get(0), valueObj)
  //        );
  //        nodes.add(Pair.with(result, valueObj));
  //        return result;
  //      } else if (objList.size() == 2) {
  //        this.nodeCounter++;
  //        result = new SSZCustomListType(
  //            descriptor,
  //            typeResolver,
  //            listAccessor,
  //            (Integer) objList.get(1),
  //            () -> parseCurrentType(objList.get(0), valueObj)
  //        );
  //        nodes.add(Pair.with(result, valueObj));
  //        return result;
  //      } else {
  //        throw new RuntimeException("Cannot parse " + typeObj + " type");
  //      }
  //    } else if (new
  // HashSet(Arrays.asList(typeObj.getClass().getInterfaces())).contains(Map.class)) {
  //      Map typeMap = (Map) typeObj;
  //      Map valueMap = (Map) valueObj;
  //      SSZField descriptor = new
  // SSZField(TreeHashContainerRunner.SerializableContainerMock.class, null, null, null,
  // currentName, null);
  //
  //      Supplier<List<SSZType>> childTypeSupplier = () -> {
  //        List<Map.Entry<String, String>> entries = (List<Map.Entry<String, String>>)
  // typeMap.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<String, String> entry) ->
  // {
  //          return Integer.valueOf(entry.getKey().substring(5));// "fieldXXX"
  //        })).collect(Collectors.toList());
  //        return entries.stream().map(entry -> parseCurrentType(entry.getValue(),
  // valueMap.get(entry.getKey()))).collect(Collectors.toList());
  //      };
  //      SSZContainerAccessor containerAccessor =
  //          new TestContainerAccessor(
  //              childTypeSupplier,
  //              () -> {
  //                List<Map.Entry<String, String>> entries =
  //                    (List<Map.Entry<String, String>>)
  //                        valueMap.entrySet().stream()
  //                            .sorted(
  //                                Comparator.comparingInt(
  //                                    (Map.Entry<String, String> entry) -> {
  //                                      return Integer.valueOf(
  //                                          entry.getKey().substring(5)); // "fieldXXX"
  //                                    }))
  //                            .collect(Collectors.toList());
  //                return entries.stream()
  //                    .map(
  //                        entry -> {
  //                          Object type = typeMap.get(entry.getKey());
  //                          if (type instanceof String) {
  //                            return instantiateValue(entry.getValue(), (String)
  // typeMap.get(entry.getKey()));
  //                          } else {
  //                            return null;
  //                          }
  //                        })
  //                    .collect(Collectors.toList());
  //              });
  //      SSZCustomContainerType customContainerType = new SSZCustomContainerType(typeResolver,
  // descriptor, containerAccessor,
  //          childTypeSupplier);
  //      this.nodeCounter++;
  //      result = customContainerType;
  //      nodes.add(Pair.with(result, valueObj));
  //      return result;
  //    } else if (typeObj instanceof String) {
  //      this.nodeCounter++;
  //      SSZField basic = parseBasicFieldType((String) typeObj);
  //      SSZField input = new SSZField(
  //          basic.getRawClass(),
  //          basic.getFieldAnnotation(),
  //          basic.getExtraType(),
  //          basic.getExtraSize(),
  //          currentName,
  //          null
  //      );
  //      result = typeResolver.resolveSSZType(input);
  //      nodes.add(Pair.with(result, valueObj));
  //      return result;
  //    } else {
  //      throw new RuntimeException("Cannot parse " + typeObj + " type");
  //    }
  //  }

  static class SSZCustomContainerType extends SSZContainerType {
    Supplier<List<SSZType>> elementTypeSupplier;
    List<SSZType> types = null;

    public SSZCustomContainerType(
        TypeResolver typeResolver,
        SSZField descriptor,
        SSZContainerAccessor accessor,
        Supplier<List<SSZType>> elementTypeSupplier) {
      super(typeResolver, descriptor, accessor);
      this.elementTypeSupplier = elementTypeSupplier;
    }

    private void runSupplier() {
      if (types != null) {
        return;
      }

      types = elementTypeSupplier.get();
    }

    @Override
    public List<SSZType> getChildTypes() {
      runSupplier();
      return types;
    }

    @Override
    public int getChildrenCount(Object value) {
      runSupplier();
      return types.size();
    }

    @Override
    public Object getChild(Object value, int idx) {
      return super.getChild(value, idx);
    }
  }

  static class TestContainerAccessor implements SSZContainerAccessor {
    private Supplier<List<SSZType>> elementTypeSupplier;
    private Supplier<List<Object>> elementSupplier;
    private List<Object> elements;

    public TestContainerAccessor(
        Supplier<List<SSZType>> elementTypeSupplier, Supplier<List<Object>> elementSupplier) {
      this.elementTypeSupplier = elementTypeSupplier;
      this.elementSupplier = elementSupplier;
    }

    @Override
    public ContainerAccessor getAccessor(SSZField containerDescriptor) {
      return new ContainerAccessor() {
        @Override
        public List<SSZField> getChildDescriptors() {
          return elementTypeSupplier.get().stream()
              .map(SSZType::getTypeDescriptor)
              .collect(Collectors.toList());
        }

        private void cacheElements() {
          if (elements == null) {
            elements = elementSupplier.get();
          }
        }

        @Override
        public Object getChildValue(Object compositeInstance, int childIndex) {
          cacheElements();
          return elements.get(childIndex);
        }
      };
    }

    @Override
    public ContainerInstanceBuilder createInstanceBuilder(SSZField containerDescriptor) {
      return null;
    }

    @Override
    public boolean isSupported(SSZField field) {
      return field.getRawClass().equals(TreeHashContainerRunner.SerializableContainerMock.class);
    }
  }

  static class TestListAccessor implements SSZListAccessor {
    private Supplier<SSZType> elementTypeSupplier;
    private Supplier<List<Object>> elementSupplier;
    private List<Object> elements;

    public TestListAccessor(
        Supplier<SSZType> elementTypeSupplier, Supplier<List<Object>> elementSupplier) {
      this.elementTypeSupplier = elementTypeSupplier;
      this.elementSupplier = elementSupplier;
    }

    private void cacheElements() {
      if (elements == null) {
        elements = elementSupplier.get();
      }
    }

    @Override
    public int getChildrenCount(Object value) {
      cacheElements();
      return elements.size();
    }

    @Override
    public Object getChildValue(Object value, int idx) {
      cacheElements();
      return elements.get(idx);
    }

    @Override
    public SSZField getListElementType(SSZField field) {
      return elementTypeSupplier.get().getTypeDescriptor();
    }

    @Override
    public ListInstanceBuilder createInstanceBuilder(SSZField containerDescriptor) {
      return null;
    }

    @Override
    public boolean isSupported(SSZField field) {
      return field.getRawClass().equals(TreeHashContainerRunner.SerializableContainerMock.class);
    }
  }

  @SSZSerializable
  static class MainMock {}

  class CustomTypeResolver extends SimpleTypeResolver {
    private AccessorResolver accessorResolver;

    public CustomTypeResolver(
        AccessorResolver accessorResolver, ExternalVarResolver externalVarResolver) {
      super(accessorResolver, externalVarResolver);
      this.accessorResolver = accessorResolver;
    }

    @Override
    public SSZType resolveSSZType(SSZField descriptor) {
      if (descriptor.getRawClass().equals(MainMock.class)) {
        return rootType;
      } else {
        return super.resolveSSZType(descriptor);
      }
    }
  }
}
