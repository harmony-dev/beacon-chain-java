package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.runner.shuffle.ShuffleRunner;
import org.ethereum.beacon.test.type.shuffle.ShuffleTest;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ShuffleTests extends TestUtils {
  private String TESTS_DIR = "shuffling";
  private BeaconChainSpec spec;

  public ShuffleTests() {
    // xxx EPOCH_LENGTH = 2**6  # 64 slots, 6.4 minutes
    // xxx FAR_FUTURE_EPOCH = 2**64 - 1  # uint64 max
    // xxx SHARD_COUNT = 2**10  # 1024
    // xxx TARGET_COMMITTEE_SIZE = 2**7  # 128 validators
    // xxx ENTRY_EXIT_DELAY = 2**2 # 4 epochs
    SpecConstants specConstants =
        new SpecConstants() {
          @Override
          public EpochNumber getFarFutureEpoch() {
            return EpochNumber.castFrom(UInt64.MAX_VALUE);
          }

          @Override
          public ValidatorIndex getTargetCommitteeSize() {
            return ValidatorIndex.of(128);
          }

          @Override
          public SlotNumber.EpochLength getSlotsPerEpoch() {
            return new SlotNumber.EpochLength(UInt64.valueOf(1 << 6));
          }

          @Override
          public ShardNumber getShardCount() {
            return ShardNumber.of(1024);
          }

          @Override
          public EpochNumber getActivationExitDelay() {
            return EpochNumber.of(4);
          }
        };
    this.spec = BeaconChainSpec.createWithDefaultHasher(specConstants);
  }

  @Test
  public void testShuffling() {
    Path sszTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
    runTestsInResourceDir(
        sszTestsPath,
        ShuffleTest.class,
        testCase -> {
          ShuffleRunner testCaseRunner =
              new ShuffleRunner(
                  testCase,
                  spec,
                  objects ->
                      spec.get_shuffling(
                          objects.getValue0(), objects.getValue1(), objects.getValue2()));
          return testCaseRunner.run();
        });
  }

  /**
   * Runs tests on optimized version of get_shuffling, {@link BeaconChainSpec#get_shuffling2(Hash32,
   * ReadList, EpochNumber)}
   */
  @Test
  public void testShuffling2() {
    Path sszTestsPath = Paths.get(PATH_TO_TESTS, TESTS_DIR);
    runTestsInResourceDir(
        sszTestsPath,
        ShuffleTest.class,
        testCase -> {
          ShuffleRunner testCaseRunner =
              new ShuffleRunner(
                  testCase,
                  spec,
                  objects ->
                      spec.get_shuffling2(
                          objects.getValue0(), objects.getValue1(), objects.getValue2()));
          return testCaseRunner.run();
        });
  }
}
