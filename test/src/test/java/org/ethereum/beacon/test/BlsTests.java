package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.test.runner.bls.BlsAggregatePubKeys;
import org.ethereum.beacon.test.runner.bls.BlsAggregateSigs;
import org.ethereum.beacon.test.runner.bls.BlsMessageHash;
import org.ethereum.beacon.test.runner.bls.BlsMessageHashCompressed;
import org.ethereum.beacon.test.runner.bls.BlsPrivateToPublic;
import org.ethereum.beacon.test.runner.bls.BlsSignMessage;
import org.ethereum.beacon.test.type.bls.BlsTest;
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
  private BeaconChainSpec spec;

  public BlsTests() {
    this.spec = BeaconChainSpec.createWithDefaults();
  }

  @Test
  @Ignore("Fixtures uses Jacobian coordinates, testBlsMessageHashCompressed covers same cases")
  public void testBlsMessageHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, FILENAME);
    BlsTest test = readTest(getResourceFile(testFilePath.toString()), BlsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test.buildBlsMessageHashTest(),
            testCase -> {
              BlsMessageHash testRunner = new BlsMessageHash(testCase, spec);
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
                  new BlsMessageHashCompressed(testCase, spec);
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
              BlsPrivateToPublic testRunner = new BlsPrivateToPublic(testCase, spec);
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
              BlsSignMessage testRunner = new BlsSignMessage(testCase, spec);
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
              BlsAggregateSigs testRunner = new BlsAggregateSigs(testCase, spec);
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
              BlsAggregatePubKeys testRunner = new BlsAggregatePubKeys(testCase, spec);
              return testRunner.run();
            },
            UniversalTest.class);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }
}
