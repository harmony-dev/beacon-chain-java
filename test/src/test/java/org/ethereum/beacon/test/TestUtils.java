package org.ethereum.beacon.test;

import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethereum.beacon.test.type.NamedTestCase;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

public class TestUtils {
  static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  String PATH_TO_TESTS = "eth2.0-tests";
  private static final String GIT_COMMAND = "git submodule init & git submodule update --recursive --remote";

  static File getResourceFile(String relativePath) {
    try {
      String path = Resources.getResource(relativePath).getPath();
      final Path filePath = Paths.get(path);
      return filePath.toFile();
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(
          String.format(
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n %s",
              relativePath, GIT_COMMAND),
          e);
    }
  }

  static List<File> getResourceFiles(String dir) {
    try {
      String fixturesRoot = Resources.getResource(dir).getPath();
      final Path fixturesRootPath = Paths.get(fixturesRoot);
      return Files.walk(fixturesRootPath)
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

  static <V extends TestSkeleton> V readTest(File file, Class<? extends V> clazz) {
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

    V test = readTest(content, clazz);

    return test;
  }

  static <V extends TestSkeleton> Optional<String> runAllTestsInFile(
      File file, Function<TestCase, Optional<String>> testCaseRunner, Class<? extends V> clazz) {
    return runAllTestsInFile(file, testCaseRunner, clazz, Collections.emptySet());
  }

  static <V extends TestSkeleton> Optional<String> runAllTestsInFile(
      File file, Function<TestCase, Optional<String>> testCaseRunner, Class<? extends V> clazz,
      Collection<String> exclusions) {
    V test = readTest(file, clazz);
    return runAllCasesInTest(test, testCaseRunner, clazz, exclusions);
  }
  static <V extends TestSkeleton> Optional<String> runAllCasesInTest(
      V test, Function<TestCase, Optional<String>> testCaseRunner, Class<? extends V> clazz) {
    return runAllCasesInTest(test, testCaseRunner, clazz, Collections.emptySet());
  }

  static <V extends TestSkeleton> Optional<String> runAllCasesInTest(
      V test, Function<TestCase, Optional<String>> testCaseRunner, Class<? extends V> clazz,
      Collection<String> exclusions) {
    StringBuilder errors = new StringBuilder();
    AtomicInteger failed = new AtomicInteger(0);
    int total = 0;
    for (TestCase testCase : test.getTestCases()) {
      ++total;
      String name = testCase instanceof NamedTestCase
          ? ((NamedTestCase) testCase).getName()
          : "Test #" + (total - 1);
      if (exclusions.contains(name)) {
        System.out.println(String.format("[ ] %s ignored", name));
        continue;
      }

      long s = System.nanoTime();
      Optional<String> err = runTestCase(testCase, test, testCaseRunner);
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
      TestCase testCase, V test, Function<TestCase, Optional<String>> testCaseRunner) {
    Optional<String> testCaseErrors = testCaseRunner.apply(testCase);
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

  static <V> V readTest(String content, Class<? extends V> clazz) {
    try {
      return yamlMapper.readValue(content, clazz);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error thrown when reading stream with YAML reader:\n%s", e.getMessage()),
          e);
    }
  }

  static <V extends TestSkeleton> void runTestsInResourceDir(
      Path dir, Class<? extends V> testsType, Function<TestCase, Optional<String>> testCaseRunner) {
    runTestsInResourceDirImpl(dir, testsType, testCaseRunner, Collections.emptySet());
  }

  static <V extends TestSkeleton> void runTestsInResourceDir(
      Path dir,
      Class<? extends V> testsType,
      Function<TestCase, Optional<String>> testCaseRunner,
      Ignored ignored) {
    runTestsInResourceDirImpl(dir, testsType, testCaseRunner, ignored.testCases);
  }

  private static <V extends TestSkeleton> void runTestsInResourceDirImpl(
      Path dir, Class<? extends V> testsType, Function<TestCase, Optional<String>> testCaseRunner,
      Collection<String> exclusions) {
    List<File> files = getResourceFiles(dir.toString());
    boolean failed = false;
    for (File file : files) {
      System.out.println("Running tests in " + file.getName());
      Optional<String> result = runAllTestsInFile(file, testCaseRunner, testsType, exclusions);
      if (result.isPresent()) {
        System.out.println(result.get());
        System.out.println("\n----===----\n");
        failed = true;
      }
    }
    assertFalse(failed);
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
