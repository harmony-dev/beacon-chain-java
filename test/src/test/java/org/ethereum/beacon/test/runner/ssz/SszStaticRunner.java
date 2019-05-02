package org.ethereum.beacon.test.runner.ssz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.creator.ConstructorObjCreator;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.runner.ssz.mapper.ObjectSerializer;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszStaticCase;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * SSZ, Hash root, signing root tests for known types
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md</a>
 */
public class SszStaticRunner implements Runner {
  private SszStaticCase testCase;
  private SSZSerializer sszSerializer;
  private ObjectMapper yamlMapper;
  private BeaconChainSpec spec;
  private Map<Class, ObjectSerializer> objectSerializers = new HashMap<>();
  private Set<ClassInfo> coreTypes;

  public SszStaticRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszStaticCase)) {
      throw new RuntimeException("TestCase runner accepts only SszStaticCase.class as input!");
    }
    this.testCase = (SszStaticCase) testCase;
    SSZBuilder builder = new SSZBuilder();
    this.sszSerializer =
        builder
            .withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
            .buildSerializer();
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    this.spec = spec;

    // objectSerializers filling
    Set<ClassInfo> availableSerializers =
        findClassesInPackage("org.ethereum.beacon.test.runner.ssz.mapper");
    availableSerializers.stream()
        .map(this::fromInfo)
        .map((Class mapperType) -> createInstance(mapperType, yamlMapper))
        .forEach(
            m -> {
              if (m == null) {
                return; // Couldn't create instance, for example, for interface, so it's not a
                // serializer
              }
              ObjectSerializer mapper = (ObjectSerializer) m;
              objectSerializers.put(mapper.accepts(), mapper);
            });

    // Filling set with spec types
    this.coreTypes = findClassesInPackage("org.ethereum.beacon.core");
  }

  /** Instantiating {@link ObjectSerializer} */
  private ObjectSerializer createInstance(
      Class<? extends ObjectSerializer> mapperType, ObjectMapper jacksonMapper) {
    return ConstructorObjCreator.createInstanceWithConstructor(
        mapperType, new Class[] {ObjectMapper.class}, new Object[] {jacksonMapper});
  }

  private Set<ClassInfo> findClassesInPackage(String packageName) {
    ClassPath classPath = null;
    try {
      classPath = ClassPath.from(getClass().getClassLoader());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Set<ClassInfo> classes = classPath.getTopLevelClassesRecursive(packageName);

    return classes;
  }

  /** {@link ClassInfo} -> {@link Class} mapper */
  private Class fromInfo(ClassInfo classInfo) {
    Class valueType = null;
    try {
      valueType = Class.forName(classInfo.getName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    return valueType;
  }

  private Class findSpecClassByName(String name) {
    final String searchName;
    if (name.equals("Validator")) {
      searchName = "ValidatorRecord"; // XXX: we have different naming
    } else if (name.equals("BeaconState")) {
      searchName = "BeaconStateImpl"; // XXX: we have several implementations
    } else {
      searchName = name;
    }
    List<ClassInfo> classes =
        coreTypes.stream()
            .filter((type) -> type.getSimpleName().equals(searchName))
            .collect(Collectors.toList());
    ClassInfo valueTypeInfo;
    if (classes.size() != 1) {
      throw new RuntimeException(
          String.format(
              "Failed: should be only one appropriate core class for %s (%s classes found) ",
              name, classes.size()));
    } else {
      valueTypeInfo = classes.get(0);
    }

    return fromInfo(valueTypeInfo);
  }

  public Optional<String> run() {
    Class valueType = findSpecClassByName(testCase.getTypeName());
    Object fromSerialized =
        sszSerializer.decode(BytesValue.fromHexString(testCase.getSerialized()), valueType);
    Object simplified = objectSerializers.get(valueType).map(fromSerialized);
    ObjectNode expectedValue = yamlMapper.convertValue(testCase.getValue(), ObjectNode.class);
    // XXX: expected goes second as our constructed node contains with overridden `equals` operators
    // which should be used in comparison
    assertEquals(simplified, expectedValue);
    assertHexStrings(testCase.getRoot(), spec.hash_tree_root(fromSerialized).toString());
    if (testCase.getSigningRoot() != null) {
      assertHexStrings(testCase.getSigningRoot(), spec.signing_root(fromSerialized).toString());
    }

    return Optional.empty();
  }
}
