package org.ethereum.beacon.test.runner.ssz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.creator.ConstructorObjCreator;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.runner.ssz.mapper.ObjectSerializer;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszStaticCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertHexStrings;

/**
 * SSZ, Hash root, signing root tests for known types
 *
 * <p>Test format description: <a href="https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md">https://github.com/ethereum/eth2.0-specs/blob/dev/specs/test_formats/ssz_static/core.md</a>
 */
public class SszStaticRunner implements Runner {
  private SszStaticCase testCase;
  private SSZObjectHasher baseHasher;
  private SSZObjectHasher incHasher;
  private SSZSerializer sszSerializer;
  private ObjectMapper yamlMapper;
  private BeaconChainSpec spec;
  private Map<Class, ObjectSerializer> objectSerializers = new HashMap<>();

  public SszStaticRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszStaticCase)) {
      throw new RuntimeException(
          "TestCase runner accepts only SszStaticCase.class as input!");
    }
    this.testCase = (SszStaticCase) testCase;
    Function<BytesValue, Hash32> hashFunction = Hashes::keccak256;
    SSZBuilder builder = new SSZBuilder();
    this.sszSerializer = builder.buildSerializer();
    SSZHasher baseSszHasher =
        new SSZBuilder().withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
            .withIncrementalHasher(false)
            .buildHasher(hashFunction);
    this.baseHasher = new SSZObjectHasher(baseSszHasher);
    SSZHasher incSszHasher =
        new SSZBuilder().withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
            .withIncrementalHasher(true)
            .buildHasher(hashFunction);
    this.incHasher = new SSZObjectHasher(incSszHasher);
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    Set<ClassInfo> mappers = findClassesInPackage("org.ethereum.beacon.test.runner.ssz.mapper");
    mappers.stream().map(this::fromInfo).map((Class mapperType) -> createInstance(mapperType, yamlMapper)).forEach(m -> {
      if (m == null) {
        return; // Couldn't create instance, for example, for interface
      }
      ObjectSerializer mapper = (ObjectSerializer) m;
      objectSerializers.put(mapper.accepts(), mapper);
    });
    this.spec = spec;
  }

  private ObjectSerializer createInstance(Class<? extends ObjectSerializer> mapperType, ObjectMapper jacksonMapper) {
    return ConstructorObjCreator.createInstanceWithConstructor(
        mapperType,
        new Class[] {ObjectMapper.class},
        new Object[] {jacksonMapper}
    );
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

  private Class fromInfo(ClassInfo classInfo) {
    Class valueType = null;
    try {
      valueType = Class.forName(classInfo.getName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    return valueType;
  }

  public Optional<String> run() {
    Set<ClassInfo> coreTypes = findClassesInPackage("org.ethereum.beacon.core");
    List<ClassInfo> classes = coreTypes.stream().filter((type) -> type.getSimpleName().endsWith(testCase.getTypeName())).collect(Collectors.toList());
    if (classes.size() != 1) {
      return Optional.of(String.format("Failed: should be only one appropriate core class for %s (%s classes found) ", testCase.getTypeName(), classes.size()));
    }
    ClassInfo valueTypeInfo = classes.get(0);
    Class valueType = fromInfo(valueTypeInfo);
    Object fromSerialized = sszSerializer.decode(BytesValue.fromHexString(testCase.getSerialized()), valueType);
    Object simplified = objectSerializers.get(valueType).map(fromSerialized);
    ObjectNode expectedValue = yamlMapper.convertValue(testCase.getValue(), ObjectNode.class);
    // XXX: expected goes second as our constructed node contains with overridden `equals` operators
    // which should be used in comparison
    assertEquals(simplified, expectedValue);
    assertHexStrings(testCase.getRoot(), baseHasher.getHash(fromSerialized).toString());
    assertHexStrings(testCase.getRoot(), incHasher.getHash(fromSerialized).toString());
    if (testCase.getSigningRoot() != null) {
      assertHexStrings(testCase.getSigningRoot(), spec.signed_root(fromSerialized).toString());
    }

    return Optional.empty();
  }
}
