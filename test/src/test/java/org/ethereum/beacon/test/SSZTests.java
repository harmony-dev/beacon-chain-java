package org.ethereum.beacon.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.ethereum.beacon.test.type.SSZ;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.fail;

public class SSZTests extends GitHubTests {
  String TESTS_DIR = "ssz";

  @Test
  public void test() {
    Path sszTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
    List<String> files = getFiles(sszTestsPath.toString());
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    for (String file : files) {
      Path path = Paths.get("/", sszTestsPath.toString(), file);
      InputStream inputStream = ClassLoader.class.getResourceAsStream(path.toString());
      String content;
      try (InputStreamReader streamReader = new InputStreamReader(inputStream, Charsets.UTF_8)) {
        content = CharStreams.toString(streamReader);
      } catch (IOException e) {
        throw new RuntimeException(
            String.format("Error reading contents of stream %s", inputStream), e);
      }

      try {
        SSZ testCase = yamlMapper.readValue(content, SSZ.class);

        StringBuilder errors = new StringBuilder();
        for(SSZ.SSZTestCase testCase1: testCase.getTest_cases()) {
          if (!runTestCase(testCase1)) {
            errors.append("TEST " + testCase.getSummary() + " v" + testCase.getVersion() + ": \n\tinput " + testCase1.getSsz() + " (" + String.join(", ", testCase1.getTags()) + ")");
            errors.append("\n");
          }
        }
        String errorsOut = errors.toString();
        if (!errorsOut.isEmpty()) {
          System.out.println(errorsOut);
          fail();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    System.out.println("");

  }

  public boolean runTestCase(SSZ.SSZTestCase testCase) {
    return false;
  }
}
