package org.ethereum.beacon.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.TestSkeleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

public class TestUtils {
  static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  String PATH_TO_TESTS = "eth2.0-tests";

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
              "Nothing found on path `%s`.\n Maybe you need to pull tests submodule with following command:\n git submodule update --recursive --remote",
              dir),
          e);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Failed to read files in directory `%s`.", dir), e);
    }
  }

  static <V extends TestSkeleton> Optional<String> runAllTestsInFile(
      File file, Function<TestCase, Optional<String>> testCaseRunner, Class<? extends V> clazz) {
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

    StringBuilder errors = new StringBuilder();
    for (TestCase testCase : test.getTestCases()) {
      Optional<String> testCaseErrors = testCaseRunner.apply(testCase);
      if (testCaseErrors.isPresent()) {
        errors
            .append("FAILED TEST ")
            .append(test)
            .append("\n")
            .append(testCase)
            .append("\n")
            .append("ERROR: ")
            .append(testCaseErrors.get())
            .append("\n---\n");
      }
    }

    if (errors.length() == 0) {
      return Optional.empty();
    }

    return Optional.of(errors.toString());
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
    List<File> files = getResourceFiles(dir.toString());
    for (File file : files) {
      System.out.println("Running tests in " + file.getName());
      Optional<String> result = runAllTestsInFile(file, testCaseRunner, testsType);
      if (result.isPresent()) {
        System.out.println(result.get());
        fail();
      }
    }
  }
}
