package org.ethereum.beacon.db.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public abstract class FileUtil {
  private FileUtil() {}

  public static void removeRecursively(String path) throws IOException {
    if (!Files.exists(Paths.get(path))) {
      return;
    }

    Files.walk(Paths.get(path))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }
}
