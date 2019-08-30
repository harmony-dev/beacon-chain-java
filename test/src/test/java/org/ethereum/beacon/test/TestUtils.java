package org.ethereum.beacon.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.spec.SpecConstantsResolver;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsDataMerged;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.chainspec.SpecDataUtils;
import org.ethereum.beacon.emulator.config.chainspec.SpecHelpersData;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.test.type.DataMapperTestCase;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszGenericCase;
import org.ethereum.beacon.test.type.ssz.SszStaticCase;
import org.ethereum.beacon.test.type.state.field.BlsSettingField;
import org.ethereum.beacon.util.Objects;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;

public class TestUtils {
  private static final String GIT_COMMAND =
      "git submodule init & git submodule update --recursive --remote";
  private static final Schedulers schedulers = Schedulers.createDefault();
  static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  static String PATH_TO_TESTS = "eth2.0-spec-tests/tests";
  static Integer CASE_DIR_LEVEL = 2;
  static Path MAINNET_TESTS = Paths.get(PATH_TO_TESTS, "mainnet");
  static Path MINIMAL_TESTS = Paths.get(PATH_TO_TESTS, "minimal");

  /** List of directories exactly levelDeeper deeper from input dir */
  private static List<File> getResourceDirs(String dir, int levelDeeper) {
    final Path fixturesRootPath;
    try {
      fixturesRootPath = Paths.get(Resources.getResource(dir).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(
          String.format(
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n %s",
              dir, GIT_COMMAND),
          e);
    }
    return getDirs(fixturesRootPath, levelDeeper);
  }

  private static List<File> getDirs(Path dir, int levelDeeper) {
    try {
      Set<Path> pathsOneLevelEarlier =
          Files.walk(dir, levelDeeper - 1).filter(Files::isDirectory).collect(Collectors.toSet());
      return Files.walk(dir, levelDeeper)
          .filter(Files::isDirectory)
          .filter(d -> !pathsOneLevelEarlier.contains(d))
          .map(Path::toFile)
          .collect(Collectors.toList());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(
          String.format(
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n %s",
              dir, GIT_COMMAND),
          e);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Failed to read directories in directory `%s`.", dir), e);
    }
  }

  /** List of all files in input directory */
  private static List<File> getFiles(File dir) {
    try {
      return Files.walk(dir.toPath())
          .filter(Files::isRegularFile)
          .map(Path::toFile)
          .collect(Collectors.toList());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(
          String.format(
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n %s",
              dir, GIT_COMMAND),
          e);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to read files in directory `%s`.", dir), e);
    }
  }

  /** Maps input yaml file to class */
  private static <V> V readYamlFile(File file, Class<? extends V> clazz) {
    String content = new String(readFile(file).extractArray(), Charsets.UTF_8);
    return parseYamlData(content, clazz);
  }

  /** Maps yaml string to class */
  private static <V> V parseYamlData(String content, Class<? extends V> clazz) {
    try {
      return yamlMapper.readValue(content, clazz);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error thrown when reading stream with YAML reader:\n%s", e.getMessage()),
          e);
    }
  }

  /** Reads file to string */
  private static BytesValue readFile(File file) {
    try {
      return BytesValue.wrap(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error reading contents of file: %s", file.toPath().toString()), e);
    }
  }

  /**
   * Runs tests case with provided spec using supplied test runner. If any errors are fired, error
   * output is returned as readable string
   */
  private static Optional<String> runTestCase(
      TestCase testCase,
      BeaconChainSpec spec,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner) {
    Optional<String> testCaseErrors;
    try {
      testCaseErrors = testCaseRunner.apply(Pair.with(testCase, spec));
    } catch (Exception ex) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      ex.printStackTrace(pw);
      String error =
          String.format("Unexpected error when running case %s: %s", testCase, sw.toString());
      testCaseErrors = Optional.of(error);
    }
    if (testCaseErrors.isPresent()) {
      StringBuilder errors = new StringBuilder();
      errors
          .append("FAILED TEST ")
          .append(testCase)
          .append("\n")
          .append("ERROR: ")
          .append(testCaseErrors.get())
          .append("\n---\n");

      return Optional.of(errors.toString());
    }

    return Optional.empty();
  }

  /**
   * Just a shortcut of {@link #runSpecTestsInResourceDir(Path, Path, Class, Function)} to run pair
   * of tests with different specs together
   */
  static <V extends DataMapperTestCase> void runSpecTestsInResourceDirs(
      Path rootDir1,
      Path rootDir2,
      Path subDir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner) {
    runSpecTestsInResourceDir(rootDir1, subDir, testsType, testCaseRunner, Ignored.EMPTY, false);
    runSpecTestsInResourceDir(rootDir2, subDir, testsType, testCaseRunner, Ignored.EMPTY, false);
  }

  /**
   * Runs tests which requires BeaconChainSpec for execution in provided resource dir
   *
   * @param rootDir Root dir from resources folder, spec constant `config.yaml` is here
   * @param subDir Sub directory with test directories, relative to rootDir
   * @param testsType Test case type
   * @param testCaseRunner Test case runner, supports test case type
   * @param <V> Any kind of test case that uses set of file strings to load data
   */
  static <V extends DataMapperTestCase> void runSpecTestsInResourceDir(
      Path rootDir,
      Path subDir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner) {
    runSpecTestsInResourceDir(rootDir, subDir, testsType, testCaseRunner, Ignored.EMPTY, false);
  }

  /**
   * Runs tests which requires BeaconChainSpec for execution in provided resource dir
   *
   * @param rootDir Root dir from resources folder, spec constant `config.yaml` is here
   * @param subDir Sub directory with test directories, relative to rootDir
   * @param testsType Test case type
   * @param testCaseRunner Test case runner, supports test case type
   * @param ignored list of ignored cases
   * @param parallel whether to run tests in parallel
   * @param <V> Any kind of test case that uses set of file strings to load data
   */
  public static <V extends DataMapperTestCase> void runSpecTestsInResourceDir(
      Path rootDir,
      Path subDir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Ignored ignored,
      boolean parallel) {
    String subDirString = Paths.get(rootDir.toString(), subDir.toString()).toString();
    List<File> dirs = getResourceDirs(subDirString, CASE_DIR_LEVEL);
    Collection<String> dirNamesExclusions = ignored.fileNames;
    boolean isCI = Boolean.parseBoolean(System.getenv("CI"));
    if (ignored.forCI && !isCI) {
      dirNamesExclusions = Collections.emptySet();
    }
    Scheduler scheduler =
        parallel ? schedulers.cpuHeavy() : schedulers.newSingleThreadDaemon("tests");
    AtomicBoolean failed = new AtomicBoolean(false);
    System.out.printf(
        "Running tests in %s with parallel execution set as %s%n", subDirString, parallel);
    AtomicInteger counter = new AtomicInteger(1);
    List<CompletableFuture> tasks = new ArrayList<>();
    SpecConstantsData specConstantsData =
        loadSpecFromResourceFile(Paths.get(rootDir.toString(), "config.yaml"));
    for (File dir : dirs) {
      if (dirNamesExclusions.contains(dir.getName())) {
        System.out.println(String.format("Skipping dir %s (in exclusions)", dir.getName()));
        continue;
      }
      Runnable task =
          () -> {
            Optional<String> result;
            int num = counter.getAndIncrement();
            try {
              Class[] paramTypes = new Class[] {Map.class, ObjectMapper.class, String.class};
              Map<String, BytesValue> filesAndData = new HashMap<>();
              for (File file : getFiles(dir)) {
                BytesValue content = readFile(file);
                filesAndData.put(file.getName(), content);
              }
              Object[] params = new Object[] {filesAndData, yamlMapper, dir.getName()};
              DataMapperTestCase testCase =
                  testsType.getConstructor(paramTypes).newInstance(params);
              BeaconChainSpec spec = createSpecForTest(testCase, specConstantsData);
              SSZSerializer ssz =
                  new SSZBuilder()
                      .withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
                      .withExtraObjectCreator(SpecConstants.class, spec.getConstants())
                      .buildSerializer();
              testCase.setSszSerializer(ssz);
              result = runTestCase(testCase, spec, testCaseRunner);
            } catch (Exception e) {
              result = Optional.of("Cannot create testcase, exception thrown " + e);
            }
            StringBuilder output = new StringBuilder();
            output.append(num).append(". Running tests in ").append(dir.getName()).append("... ");
            if (result.isPresent()) {
              output.append("FAILED\n");
              output.append(num).append(". ").append(result.get()).append('\n');
              failed.set(true);
            } else {
              output.append("OK\n");
            }
            System.out.print(output.toString());
          };
      tasks.add(scheduler.executeR(task));
    }

    CompletableFuture[] cfs = tasks.toArray(new CompletableFuture[] {});
    CompletableFuture.allOf(cfs).join();
    assertFalse(failed.get());
  }

  /**
   * Runs static type ssz tests which requires BeaconChainSpec for execution in provided resource
   * dir
   *
   * @param rootDir Root dir from resources folder, spec constant `config.yaml` is here
   * @param subDir Sub directory with test directories, relative to rootDir
   * @param testCaseRunner Test case runner, supports test case type
   * @param ignored list of ignored cases
   * @param parallel whether to run tests in parallel
   * @param <V> Any kind of test case that uses set of file strings to load data
   */
  public static <V extends DataMapperTestCase> void runSszStaticTestsInResourceDir(
      Path rootDir,
      Path subDir,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Ignored ignored,
      boolean parallel) {
    String subDirString = Paths.get(rootDir.toString(), subDir.toString()).toString();
    List<File> typeDirs = getResourceDirs(subDirString, 1);
    Collection<String> dirNamesExclusions = ignored.fileNames;
    boolean isCI = Boolean.parseBoolean(System.getenv("CI"));
    if (ignored.forCI && !isCI) {
      dirNamesExclusions = Collections.emptySet();
    }
    Scheduler scheduler =
        parallel ? schedulers.cpuHeavy() : schedulers.newSingleThreadDaemon("tests");
    AtomicBoolean failed = new AtomicBoolean(false);
    System.out.printf(
        "Running tests in %s with parallel execution set as %s%n", subDirString, parallel);
    AtomicInteger counter = new AtomicInteger(1);
    SpecConstantsData specConstantsData =
        loadSpecFromResourceFile(Paths.get(rootDir.toString(), "config.yaml"));
    for (File typeDir : typeDirs) {
      if (dirNamesExclusions.contains(typeDir.getName())) {
        System.out.println(String.format("Skipping dir %s (in exclusions)", typeDir.getName()));
        continue;
      }
      String typeName = typeDir.getName();
      List<CompletableFuture> tasks = new ArrayList<>();
      for (File caseDir : getDirs(typeDir.toPath(), 2)) {
        Runnable task =
            () -> {
              Optional<String> result;
              int num = counter.getAndIncrement();
              String description =
                  String.format(
                      "%s/%s/%s", typeName, caseDir.getParentFile().getName(), caseDir.getName());
              try {
                Map<String, BytesValue> filesAndData = new HashMap<>();
                for (File file : getFiles(caseDir)) {
                  BytesValue content = readFile(file);
                  filesAndData.put(file.getName(), content);
                }
                SszStaticCase testCase =
                    new SszStaticCase(filesAndData, yamlMapper, typeName, description);
                BeaconChainSpec spec = createSpecForTest(testCase, specConstantsData);
                SSZSerializer ssz =
                    new SSZBuilder()
                        .withExternalVarResolver(new SpecConstantsResolver(spec.getConstants()))
                        .withExtraObjectCreator(SpecConstants.class, spec.getConstants())
                        .buildSerializer();
                testCase.setSszSerializer(ssz);
                result = runTestCase(testCase, spec, testCaseRunner);
              } catch (Exception e) {
                result = Optional.of("Cannot create testcase, exception thrown " + e);
              }
              StringBuilder output = new StringBuilder();
              output.append(String.format("%d. Running tests in %s... ", num, description));
              if (result.isPresent()) {
                output.append("FAILED\n");
                output.append(num).append(". ").append(result.get());
                failed.set(true);
              } else {
                output.append("OK\n");
              }
              System.out.print(output.toString());
            };
        tasks.add(scheduler.executeR(task));
      }
      CompletableFuture[] cfs = tasks.toArray(new CompletableFuture[] {});
      CompletableFuture.allOf(cfs).join();
      assertFalse(failed.get());
    }
  }

  /**
   * Runs generic type ssz tests which requires BeaconChainSpec for execution in provided resource
   * dir
   *
   * @param dir Sub directory with test directories by type
   * @param testCaseRunner Test case runner, supports test case type
   * @param ignored list of ignored cases
   * @param parallel whether to run tests in parallel
   * @param <V> Any kind of test case that uses set of file strings to load data
   */
  public static <V extends DataMapperTestCase> void runSszGenericTestsInResourceDir(
      Path dir,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Ignored ignored,
      boolean parallel) {
    Collection<String> dirNamesExclusions = ignored.fileNames;
    boolean isCI = Boolean.parseBoolean(System.getenv("CI"));
    if (ignored.forCI && !isCI) {
      dirNamesExclusions = Collections.emptySet();
    }
    Scheduler scheduler =
        parallel ? schedulers.cpuHeavy() : schedulers.newSingleThreadDaemon("tests");
    AtomicBoolean failed = new AtomicBoolean(false);
    System.out.printf("Running tests in %s with parallel execution set as %s%n", dir, parallel);
    AtomicInteger counter = new AtomicInteger(1);
    List<CompletableFuture> tasks = new ArrayList<>();
    for (File caseDir : getResourceDirs(dir.toString(), 2)) {
      AtomicBoolean inExclusions = new AtomicBoolean(false);
      dirNamesExclusions.stream()
          .filter(e -> caseDir.getPath().contains(e))
          .findFirst()
          .ifPresent(s -> inExclusions.set(true));
      if (inExclusions.get()) {
        System.out.println(
            String.format(
                "Skipping case %s/%s/%s (in exclusions)",
                dir.getFileName(), caseDir.getParentFile().getName(), caseDir.getName()));
        continue;
      }
      Runnable task =
          () -> {
            Optional<String> result;
            int num = counter.getAndIncrement();
            String description =
                String.format(
                    "%s/%s/%s",
                    dir.getFileName(), caseDir.getParentFile().getName(), caseDir.getName());
            try {
              Map<String, BytesValue> filesAndData = new HashMap<>();
              for (File file : getFiles(caseDir)) {
                BytesValue content = readFile(file);
                filesAndData.put(file.getName(), content);
              }
              boolean isValid = "valid".equals(caseDir.getParentFile().getName());
              SszGenericCase testCase =
                  new SszGenericCase(
                      filesAndData,
                      yamlMapper,
                      dir.getFileName().toString(),
                      caseDir.getName(),
                      isValid,
                      description);
              SSZSerializer ssz = new SSZBuilder().buildSerializer();
              testCase.setSszSerializer(ssz);
              BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
              result = runTestCase(testCase, spec, testCaseRunner);
            } catch (Exception e) {
              result = Optional.of("Cannot create testcase, exception thrown " + e);
            }
            StringBuilder output = new StringBuilder();
            output.append(String.format("%d. Running tests in %s... ", num, description));
            if (result.isPresent()) {
              output.append("FAILED\n");
              output.append(num).append(". ").append(result.get());
              failed.set(true);
            } else {
              output.append("OK\n");
            }
            System.out.print(output.toString());
          };
      tasks.add(scheduler.executeR(task));
    }
    CompletableFuture[] cfs = tasks.toArray(new CompletableFuture[] {});
    CompletableFuture.allOf(cfs).join();
    assertFalse(failed.get());
  }

  /**
   * Loads {@link org.ethereum.beacon.core.spec.SpecConstants} ancestor from yaml config file which
   * is located somewhere in resource folder
   */
  private static SpecConstantsData loadSpecFromResourceFile(Path file) {
    File config;
    try {
      config = Paths.get(Resources.getResource(file.toString()).toURI()).toFile();
    } catch (IllegalArgumentException | URISyntaxException e) {
      throw new RuntimeException(
          String.format(
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n %s",
              file, GIT_COMMAND),
          e);
    }

    SpecConstantsData specConstantsDataRaw = readYamlFile(config, SpecConstantsDataMerged.class);
    SpecConstantsData specConstantsData;
    try {
      specConstantsData =
          Objects.copyProperties(
              SpecDataUtils.createSpecConstantsData(BeaconChainSpec.DEFAULT_CONSTANTS),
              specConstantsDataRaw);
    } catch (Exception ex) {
      throw new RuntimeException("Cannot merge spec constants with default settings");
    }

    return specConstantsData;
  }

  /**
   * Constructs {@link BeaconChainSpec} from {@link SpecConstantsData} with customized bls
   * verification settings
   */
  private static BeaconChainSpec fromSpecConstants(
      SpecConstantsData specConstantsData, boolean isBlsVerified) {
    SpecHelpersData specHelpersData = new SpecHelpersData();
    specHelpersData.setBlsVerify(isBlsVerified);
    specHelpersData.setVerifyDepositProof(true);
    specHelpersData.setComputableGenesisTime(true);

    SpecData specData = new SpecData();
    specData.setSpecHelpersOptions(specHelpersData);
    specData.setSpecConstants(specConstantsData);

    return new SpecBuilder().withSpec(specData).buildSpec();
  }

  /**
   * Constructs {@link BeaconChainSpec} from {@link SpecConstantsData} with customized bls
   * verification settings tied to specific test case
   */
  private static BeaconChainSpec createSpecForTest(
      DataMapperTestCase testCase, SpecConstantsData specConstantsData) {
    boolean isBlsVerified = false;
    if (testCase instanceof BlsSettingField) {
      Integer blsFlag = ((BlsSettingField) testCase).getBlsSetting();
      isBlsVerified = blsFlag != null && blsFlag < 2;
    }

    return fromSpecConstants(specConstantsData, isBlsVerified);
  }

  /**
   * Runs general format tests in directory
   *
   * @param dir Resource directory
   * @param testsType Tests class type
   * @param testCaseRunner Runner for this type
   * @param <V> any test case type
   */
  public static <V extends TestCase> void runGeneralTestsInResourceDir(
      Path dir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner) {
    runGeneralTestsInResourceDir(dir, testsType, testCaseRunner, Ignored.EMPTY, false);
  }

  /**
   * Runs general format tests in directory
   *
   * @param dir Resource directory
   * @param testsType Tests class type
   * @param testCaseRunner Runner for this type
   * @param ignored list of ignored cases
   * @param parallel whether to run tests in parallel
   * @param <V> any test case type
   */
  public static <V extends TestCase> void runGeneralTestsInResourceDir(
      Path dir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Ignored ignored,
      boolean parallel) {
    List<File> dirs = getResourceDirs(dir.toString(), CASE_DIR_LEVEL);
    boolean isCI = Boolean.parseBoolean(System.getenv("CI"));
    Collection<String> dirNamesExclusions =
        isCI == ignored.forCI ? ignored.fileNames : Collections.emptySet();
    Scheduler scheduler =
        parallel ? schedulers.cpuHeavy() : schedulers.newSingleThreadDaemon("tests");
    AtomicBoolean failed = new AtomicBoolean(false);
    System.out.printf("Running tests in %s with parallel execution set as %s%n", dir, parallel);
    AtomicInteger counter = new AtomicInteger(1);
    List<CompletableFuture> tasks = new ArrayList<>();
    for (File caseDir : dirs) {
      if (dirNamesExclusions.contains(caseDir.getName())) {
        System.out.println(String.format("Skipping dir %s (in exclusions)", caseDir.getName()));
        continue;
      }
      Runnable task =
          () -> {
            int num = counter.getAndIncrement();
            List<File> files = getFiles(caseDir);
            assert files.size() == 1;
            Optional<String> result;
            try {
              TestCase testCase = yamlMapper.readValue(files.get(0), testsType);
              BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();
              result = runTestCase(testCase, spec, testCaseRunner);
            } catch (Exception e) {
              result = Optional.of("Cannot create testcase, exception thrown " + e);
            }
            StringBuilder output = new StringBuilder();
            output
                .append(num)
                .append(". Running tests in ")
                .append(caseDir.getName())
                .append("... ");
            if (result.isPresent()) {
              output.append("FAILED\n");
              output.append(num).append(". ").append(result.get()).append('\n');
              failed.set(true);
            } else {
              output.append("OK\n");
            }
            System.out.print(output.toString());
          };
      tasks.add(scheduler.executeR(task));
    }

    CompletableFuture[] cfs = tasks.toArray(new CompletableFuture[] {});
    CompletableFuture.allOf(cfs).join();
    assertFalse(failed.get());
  }

  public static class Ignored {
    public static Ignored EMPTY =
        new Ignored(Collections.emptySet(), Collections.emptySet(), false);
    private final Set<String> testCases;
    private final Set<String> fileNames;
    private final boolean forCI;

    private Ignored(Set<String> testCases, Set<String> fileNames, boolean forCI) {
      this.testCases = testCases;
      this.fileNames = fileNames;
      this.forCI = forCI;
    }

    public static Ignored casesOf(String... testCases) {
      assert testCases.length > 0;
      return new Ignored(new HashSet<>(Arrays.asList(testCases)), new HashSet<>(), false);
    }

    public static Ignored filesOf(String... fileNames) {
      assert fileNames.length > 0;
      return new Ignored(new HashSet<>(), new HashSet<>(Arrays.asList(fileNames)), false);
    }

    public Ignored forCI() {
      return new Ignored(new HashSet<>(testCases), new HashSet<>(fileNames), true);
    }
  }
}
