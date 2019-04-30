package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.bls.BlsAggregatePubKeys;
import org.ethereum.beacon.test.runner.bls.BlsAggregateSigs;
import org.ethereum.beacon.test.runner.bls.BlsMessageHash;
import org.ethereum.beacon.test.runner.bls.BlsMessageHashCompressed;
import org.ethereum.beacon.test.runner.bls.BlsPrivateToPublic;
import org.ethereum.beacon.test.runner.bls.BlsSignMessage;
import org.ethereum.beacon.test.type.bls.BlsAggregatePubKeysTest;
import org.ethereum.beacon.test.type.bls.BlsAggregateSigsTest;
import org.ethereum.beacon.test.type.bls.BlsMessageHashCompressedTest;
import org.ethereum.beacon.test.type.bls.BlsMessageHashTest;
import org.ethereum.beacon.test.type.bls.BlsPrivateToPublicTest;
import org.ethereum.beacon.test.type.bls.BlsSignMessageTest;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.fail;

/**
 * Tests for BLS methods.
 * Test format description: <a href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/bls">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/bls</a>
 */
public class BlsTests extends TestUtils {
  private String TESTS_DIR = "bls";

  @Test
  @Ignore("Fixtures uses Jacobian coordinates, testBlsMessageHashCompressed covers same cases")
  public void testBlsMessageHash() {
    Path testFilePath =
        Paths.get(PATH_TO_TESTS, TESTS_DIR, "msg_hash_g2_uncompressed", "g2_uncompressed.yaml");
    BlsMessageHashTest test =
        readTest(getResourceFile(testFilePath.toString()), BlsMessageHashTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            input -> {
              BlsMessageHash testRunner = new BlsMessageHash(input.getValue0(), input.getValue1());
              return testRunner.run();
            },
            BlsMessageHashTest.class,
            true);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsMessageHashCompressed() {
    Path testFilePath =
        Paths.get(PATH_TO_TESTS, TESTS_DIR, "msg_hash_g2_compressed", "g2_compressed.yaml");
    BlsMessageHashCompressedTest test =
        readTest(getResourceFile(testFilePath.toString()), BlsMessageHashCompressedTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            input -> {
              BlsMessageHashCompressed testRunner =
                  new BlsMessageHashCompressed(input.getValue0(), input.getValue1());
              return testRunner.run();
            },
            BlsMessageHashCompressedTest.class,
            true);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsPrivateToPublic() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "priv_to_pub", "priv_to_pub.yaml");
    BlsPrivateToPublicTest test =
        readTest(getResourceFile(testFilePath.toString()), BlsPrivateToPublicTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            input -> {
              BlsPrivateToPublic testRunner =
                  new BlsPrivateToPublic(input.getValue0(), input.getValue1());
              return testRunner.run();
            },
            BlsPrivateToPublicTest.class,
            true);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsSignMessage() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, TESTS_DIR, "sign_msg", "sign_msg.yaml");
    BlsSignMessageTest test =
        readTest(getResourceFile(testFilePath.toString()), BlsSignMessageTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            input -> {
              BlsSignMessage testRunner = new BlsSignMessage(input.getValue0(), input.getValue1());
              return testRunner.run();
            },
            BlsSignMessageTest.class,
            true);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsAggregateSigs() {
    Path testFilePath =
        Paths.get(PATH_TO_TESTS, TESTS_DIR, "aggregate_sigs", "aggregate_sigs.yaml");
    BlsAggregateSigsTest test =
        readTest(getResourceFile(testFilePath.toString()), BlsAggregateSigsTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            input -> {
              BlsAggregateSigs testRunner =
                  new BlsAggregateSigs(input.getValue0(), input.getValue1());
              return testRunner.run();
            },
            BlsAggregateSigsTest.class,
            true);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }

  @Test
  public void testBlsAggregatePubKeys() {
    Path testFilePath =
        Paths.get(PATH_TO_TESTS, TESTS_DIR, "aggregate_pubkeys", "aggregate_pubkeys.yaml");
    BlsAggregatePubKeysTest test =
        readTest(getResourceFile(testFilePath.toString()), BlsAggregatePubKeysTest.class);
    Optional<String> errors =
        runAllCasesInTest(
            test,
            input -> {
              BlsAggregatePubKeys testRunner =
                  new BlsAggregatePubKeys(input.getValue0(), input.getValue1());
              return testRunner.run();
            },
            BlsAggregatePubKeysTest.class,
            true);
    if (errors.isPresent()) {
      System.out.println(errors.get());
      fail();
    }
  }
}
