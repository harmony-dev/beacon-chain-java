package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.test.runner.BlsAggregatePubKeys;
import org.ethereum.beacon.test.runner.BlsAggregateSigs;
import org.ethereum.beacon.test.runner.BlsMessageHash;
import org.ethereum.beacon.test.runner.BlsMessageHashCompressed;
import org.ethereum.beacon.test.runner.BlsPrivateToPublic;
import org.ethereum.beacon.test.runner.BlsSignMessage;
import org.ethereum.beacon.test.type.BlsTest;
import org.ethereum.beacon.test.type.UniversalTest;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.fail;

public class BlsTests extends TestUtils {
  private String TESTS_DIR = "bls";
  private String FILENAME = "test_bls.yml";
  private SpecHelpers specHelpers;

  public BlsTests() {
    this.specHelpers =
        new SpecHelpers(
            SpecConstants.DEFAULT, Hashes::keccak256, SSZObjectHasher.create(Hashes::keccak256));
  }

  @Test
  @Ignore("??? not matched")
  public void testBlsMessageHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, FILENAME);
    BlsTest test = readTest(getResourceFile(testFilePath.toString()), BlsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test.buildBlsMessageHashTest(),
            testCase -> {
              BlsMessageHash testRunner = new BlsMessageHash(testCase, specHelpers);
              return testRunner.run();
            },
            UniversalTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsMessageHashCompressed() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, FILENAME);
    BlsTest test = readTest(getResourceFile(testFilePath.toString()), BlsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test.buildBlsMessageHashCompressedTest(),
            testCase -> {
              BlsMessageHashCompressed testRunner =
                  new BlsMessageHashCompressed(testCase, specHelpers);
              return testRunner.run();
            },
            UniversalTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsPrivateToPublic() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, FILENAME);
    BlsTest test = readTest(getResourceFile(testFilePath.toString()), BlsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test.buildBlsPrivateToPublicTest(),
            testCase -> {
              BlsPrivateToPublic testRunner = new BlsPrivateToPublic(testCase, specHelpers);
              return testRunner.run();
            },
            UniversalTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsSignMessage() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, FILENAME);
    BlsTest test = readTest(getResourceFile(testFilePath.toString()), BlsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test.buildBlsSignMessageTest(),
            testCase -> {
              BlsSignMessage testRunner = new BlsSignMessage(testCase, specHelpers);
              return testRunner.run();
            },
            UniversalTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsAggregateSigs() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, FILENAME);
    BlsTest test = readTest(getResourceFile(testFilePath.toString()), BlsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test.buildBlsAggregateSigsTest(),
            testCase -> {
              BlsAggregateSigs testRunner = new BlsAggregateSigs(testCase, specHelpers);
              return testRunner.run();
            },
            UniversalTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsAggregatePubKeys() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, FILENAME);
    BlsTest test = readTest(getResourceFile(testFilePath.toString()), BlsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test.buildBlsAggregatePubKeysTest(),
            testCase -> {
              BlsAggregatePubKeys testRunner = new BlsAggregatePubKeys(testCase, specHelpers);
              return testRunner.run();
            },
            UniversalTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }
}
