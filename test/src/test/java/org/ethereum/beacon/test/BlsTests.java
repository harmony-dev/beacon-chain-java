package org.ethereum.beacon.test;

import org.ethereum.beacon.test.runner.bls.BlsAggregatePubKeys;
import org.ethereum.beacon.test.runner.bls.BlsAggregateSigs;
import org.ethereum.beacon.test.runner.bls.BlsMessageHash;
import org.ethereum.beacon.test.runner.bls.BlsMessageHashCompressed;
import org.ethereum.beacon.test.runner.bls.BlsPrivateToPublic;
import org.ethereum.beacon.test.runner.bls.BlsSignMessage;
import org.ethereum.beacon.test.type.bls.BlsAggregatePubKeysCase;
import org.ethereum.beacon.test.type.bls.BlsAggregateSigsCase;
import org.ethereum.beacon.test.type.bls.BlsMessageHashCase;
import org.ethereum.beacon.test.type.bls.BlsMessageHashCompressedCase;
import org.ethereum.beacon.test.type.bls.BlsPrivateToPublicCase;
import org.ethereum.beacon.test.type.bls.BlsSignMessageCase;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for BLS methods. Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/bls">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/bls</a>
 */
public class BlsTests extends TestUtils {
  private Path SUBDIR = Paths.get("general", "phase0", "bls");

  @Test
  @Ignore("Fixtures uses Jacobian coordinates, testBlsMessageHashCompressed covers same cases")
  public void testBlsMessageHash() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "msg_hash_uncompressed");
    runGeneralTestsInResourceDir(
        testFilePath,
        BlsMessageHashCase.class,
        objects -> {
          BlsMessageHash testRunner = new BlsMessageHash(objects.getValue0(), objects.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testBlsMessageHashCompressed() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "msg_hash_compressed");
    runGeneralTestsInResourceDir(
        testFilePath,
        BlsMessageHashCompressedCase.class,
        objects -> {
          BlsMessageHashCompressed testRunner =
              new BlsMessageHashCompressed(objects.getValue0(), objects.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testBlsPrivateToPublic() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "priv_to_pub");
    runGeneralTestsInResourceDir(
        testFilePath,
        BlsPrivateToPublicCase.class,
        objects -> {
          BlsPrivateToPublic testRunner =
              new BlsPrivateToPublic(objects.getValue0(), objects.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testBlsSignMessage() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "sign_msg");
    runGeneralTestsInResourceDir(
        testFilePath,
        BlsSignMessageCase.class,
        objects -> {
          BlsSignMessage testRunner = new BlsSignMessage(objects.getValue0(), objects.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testBlsAggregateSigs() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "aggregate_sigs");
    runGeneralTestsInResourceDir(
        testFilePath,
        BlsAggregateSigsCase.class,
        objects -> {
          BlsAggregateSigs testRunner =
              new BlsAggregateSigs(objects.getValue0(), objects.getValue1());
          return testRunner.run();
        });
  }

  @Test
  public void testBlsAggregatePubKeys() {
    Path testFilePath = Paths.get(PATH_TO_TESTS, SUBDIR.toString(), "aggregate_pubkeys");
    runGeneralTestsInResourceDir(
        testFilePath,
        BlsAggregatePubKeysCase.class,
        objects -> {
          BlsAggregatePubKeys testRunner =
              new BlsAggregatePubKeys(objects.getValue0(), objects.getValue1());
          return testRunner.run();
        });
  }
}
