package org.ethereum.beacon.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.chainspec.SpecDataUtils;
import org.ethereum.beacon.emulator.config.chainspec.SpecHelpersData;
import org.ethereum.beacon.test.type.NamedTestCase;
import org.ethereum.beacon.test.type.SpecConstantsDataMerged;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;
import org.ethereum.beacon.util.Objects;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class TestUtils {
  private static final String GIT_COMMAND =
      "git submodule init & git submodule update --recursive --remote";
  static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  static String PATH_TO_TESTS = "eth2.0-spec-tests/tests";
  static String PATH_TO_CONFIGS = "eth2.0-temp-test-configs";
  static String SPEC_CONFIG_DIR = "constant_presets";
  static String FORK_CONFIG_DIR = "fork_timelines";

  static File getResourceFile(String relativePath) {
    try {
      final Path filePath = Paths.get(Resources.getResource(relativePath).toURI());
      return filePath.toFile();
    } catch (IllegalArgumentException | URISyntaxException e) {
      throw new RuntimeException(
          String.format(
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n %s",
              relativePath, GIT_COMMAND),
          e);
    }
  }

  static List<File> getResourceFiles(String dir) {
    try {
      final Path fixturesRootPath = Paths.get(Resources.getResource(dir).toURI());
      return Files.walk(fixturesRootPath)
          .filter(Files::isRegularFile)
          .map(Path::toFile)
          .collect(Collectors.toList());
    } catch (IllegalArgumentException | URISyntaxException e) {
      throw new RuntimeException(
          String.format(
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n %s",
              dir, GIT_COMMAND),
          e);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to read files in directory `%s`.", dir), e);
    }
  }

  static <V extends TestSkeleton> V readTest(File file, Class<? extends V> clazz) {
    return readYamlFile(file, clazz);
  }

  private static <V> V readYamlFile(File file, Class<? extends V> clazz) {
    String content;
    try (InputStream inputStream = new FileInputStream(file);
        InputStreamReader streamReader = new InputStreamReader(inputStream, Charsets.UTF_8)) {
      content = CharStreams.toString(streamReader);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("File not found: %s", file.toPath().toString()), e);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error reading contents of file: %s", file.toPath().toString()), e);
    }

    return parseYamlData(content, clazz);
  }

  static <V extends TestSkeleton> Optional<String> runAllTestsInFile(
      File file,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Class<? extends V> clazz) {
    return runAllTestsInFile(file, testCaseRunner, clazz, Collections.emptySet());
  }

  static <V extends TestSkeleton> Optional<String> runAllTestsInFile(
      File file,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Class<? extends V> clazz,
      Collection<String> exclusions) {
    V test = readTest(file, clazz);
    return runAllCasesInTest(test, testCaseRunner, clazz, exclusions, false);
  }

  static <V extends TestSkeleton> Optional<String> runAllCasesInTest(
      V test,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Class<? extends V> clazz) {
    return runAllCasesInTest(test, testCaseRunner, clazz, Collections.emptySet(), false);
  }

  static <V extends TestSkeleton> Optional<String> runAllCasesInTest(
      V test,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Class<? extends V> clazz,
      boolean isBlsVerified) {
    return runAllCasesInTest(test, testCaseRunner, clazz, Collections.emptySet(), isBlsVerified);
  }

  static <V extends TestSkeleton> Optional<String> runAllCasesInTest(
      V test,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Class<? extends V> clazz,
      Collection<String> exclusions,
      boolean isBlsVerified) {
    StringBuilder errors = new StringBuilder();
    AtomicInteger failed = new AtomicInteger(0);
    int total = 0;
    for (TestCase testCase : test.getTestCases()) {
      ++total;
      String name =
          testCase instanceof NamedTestCase
              ? ((NamedTestCase) testCase).getName()
              : "Test #" + (total - 1);
      if (exclusions.contains(name)) {
        System.out.println(String.format("[ ] %s ignored", name));
        continue;
      }

      long s = System.nanoTime();
      BeaconChainSpec spec = loadSpecByName(test.getConfig(), isBlsVerified);
      Optional<String> err = runTestCase(testCase, spec, test, testCaseRunner);
      long completionTime = System.nanoTime() - s;

      if (err.isPresent()) {
        errors.append(err.get());
        failed.incrementAndGet();
      }

      System.out.println(
          String.format(
              "[%s] %s completed in %.3fs",
              err.isPresent() ? "F" : "P", name, completionTime / 1_000_000_000d));
    }

    if (errors.length() == 0) {
      return Optional.empty();
    }
    errors.append("\nTests failed: ");
    errors.append(failed.get());
    errors.append("\nTotal: ");
    errors.append(total);

    return Optional.of(errors.toString());
  }

  static <V extends TestSkeleton> Optional<String> runTestCase(
      TestCase testCase,
      BeaconChainSpec spec,
      V test,
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
          .append(test)
          .append("\n")
          .append(testCase)
          .append("\n")
          .append("ERROR: ")
          .append(testCaseErrors.get())
          .append("\n---\n");

      return Optional.of(errors.toString());
    }

    return Optional.empty();
  }

  static <V> V parseYamlData(String content, Class<? extends V> clazz) {
    try {
      return yamlMapper.readValue(content, clazz);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error thrown when reading stream with YAML reader:\n%s", e.getMessage()),
          e);
    }
  }

  static <V extends TestSkeleton> void runTestsInResourceDir(
      Path dir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner) {
    runTestsInResourceDirImpl(dir, testsType, testCaseRunner, Collections.emptySet(), 1);
  }

  static <V extends TestSkeleton> void runTestsInResourceDir(
      Path dir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      int threadNum) {
    runTestsInResourceDirImpl(dir, testsType, testCaseRunner, Collections.emptySet(), threadNum);
  }

  static <V extends TestSkeleton> void runTestsInResourceDir(
      Path dir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Ignored ignored) {
    runTestsInResourceDirImpl(dir, testsType, testCaseRunner, ignored.testCases, 1);
  }

  static BeaconChainSpec loadSpecByName(String name, boolean isBlsVerified) {
    Path configPath = Paths.get(PATH_TO_CONFIGS, SPEC_CONFIG_DIR, name + ".yaml");
    File config = getResourceFile(configPath.toString());

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

    SpecHelpersData specHelpersData = new SpecHelpersData();
    specHelpersData.setBlsVerify(isBlsVerified);

    SpecData specData = new SpecData();
    specData.setSpecHelpersOptions(specHelpersData);
    specData.setSpecConstants(specConstantsData);

    return new SpecBuilder().withSpec(specData).buildSpec();
  }

  private static <V extends TestSkeleton> void runTestsInResourceDirImpl(
      Path dir,
      Class<? extends V> testsType,
      Function<Pair<TestCase, BeaconChainSpec>, Optional<String>> testCaseRunner,
      Collection<String> exclusions,
      int threadNum) {
    List<File> files = getResourceFiles(dir.toString());
    AtomicBoolean failed = new AtomicBoolean(false);
    System.out.println("Running tests in " + dir + " with " + threadNum + " thread(s)");
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadNum);
    AtomicInteger counter = new AtomicInteger(0);
    CountDownLatch awaitLatch = new CountDownLatch(files.size());
    for (File file : files) {
      Runnable task =
          () -> {
            int num = counter.getAndIncrement();
            System.out.println(num + ". Running tests in " + file.getName());
            Optional<String> result =
                runAllTestsInFile(file, testCaseRunner, testsType, exclusions);
            if (result.isPresent()) {
              System.out.println(num + ". " + result.get());
              System.out.println(num + ". \n----===----\n");
              failed.set(true);
            }
            awaitLatch.countDown();
          };
      executor.execute(task);
    }

    try {
      awaitLatch.await();
    } catch (InterruptedException e) {
      System.out.println("\n Something breaks down execution, Exception goes next.\n");
      e.printStackTrace();
      fail();
    }
    assertFalse(failed.get());
  }

  public static int getRecommendedThreadCount() {
    return Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
  }

  public static class Ignored {
    private final Set<String> testCases;

    private Ignored(Set<String> testCases) {
      this.testCases = testCases;
    }

    public static Ignored of(String... testCases) {
      assert testCases.length > 0;
      return new Ignored(new HashSet<>(Arrays.asList(testCases)));
    }
  }
}
