package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.runner.shuffle.ShuffleRunner;
import org.ethereum.beacon.test.type.shuffle.ShuffleTest;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class ShuffleTests extends TestUtils {
  private String TESTS_DIR = "shuffling";
  private String TESTS_SUBDIR = "core";

  @Test
  public void testShuffling() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, TESTS_DIR, TESTS_SUBDIR);
    runTestsInResourceDir(
        testFileDir,
        ShuffleTest.class,
        input -> {
          ShuffleRunner testRunner =
              new ShuffleRunner(
                  input.getValue0(),
                  input.getValue1(),
                  objects ->
                      IntStream.range(0, objects.getValue2())
                          .mapToObj(
                              i -> {
                                UInt64 permuted_index =
                                    input
                                        .getValue1()
                                        .get_permuted_index(
                                            UInt64.valueOf(i),
                                            UInt64.valueOf(objects.getValue2()),
                                            objects.getValue1());
                                return objects.getValue0().get(permuted_index.getIntValue());
                              })
                          .collect(toList()));
          return testRunner.run();
        });
  }

  /**
   * Runs tests on optimized version of get_shuffling, like in {@link
   * BeaconChainSpec#compute_committee2(List, Hash32, int, int)}
   */
  @Test
  public void testShuffling2() {
    Path testFileDir = Paths.get(PATH_TO_TESTS, TESTS_DIR, TESTS_SUBDIR);
    runTestsInResourceDir(
        testFileDir,
        ShuffleTest.class,
        input -> {
          ShuffleRunner testRunner =
              new ShuffleRunner(
                  input.getValue0(),
                  input.getValue1(),
                  objects -> {
                    List<UInt64> permuted_indices =
                        input
                            .getValue1()
                            .get_permuted_list(objects.getValue0(), objects.getValue1());
                    return permuted_indices.subList(0, objects.getValue2()).stream()
                        .map(ValidatorIndex::new)
                        .collect(toList());
                  });
          return testRunner.run();
        });
  }
}
