package org.ethereum.beacon.test;

import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class GitHubTests {
  String PATH_TO_TESTS = "eth2.0-tests";

  static List<String> getFiles(String dir) {
    String fixturesRoot = Resources.getResource(dir).getPath();
    final Path fixturesRootPath = Paths.get(fixturesRoot);

    try {
      return Files.walk(fixturesRootPath)
          .filter(Files::isRegularFile)
          .map(path -> fixturesRootPath.relativize(path).toString())
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
