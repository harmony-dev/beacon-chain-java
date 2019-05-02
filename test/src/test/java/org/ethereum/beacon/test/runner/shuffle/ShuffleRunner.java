package org.ethereum.beacon.test.runner.shuffle;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.shuffle.ShuffleTestCase;
import org.javatuples.Triplet;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ethereum.beacon.test.SilentAsserts.assertLists;

/** TestRunner for {@link ShuffleTestCase} */
public class ShuffleRunner implements Runner {
  private ShuffleTestCase testCase;
  private BeaconChainSpec spec;
  private Function<Triplet<List<ValidatorIndex>, Hash32, Integer>, List<ValidatorIndex>>
      getShuffling;

  public ShuffleRunner(
      TestCase testCase,
      BeaconChainSpec spec,
      Function<Triplet<List<ValidatorIndex>, Hash32, Integer>, List<ValidatorIndex>>
          getShuffling) {
    if (!(testCase instanceof ShuffleTestCase)) {
      throw new RuntimeException("TestCase runner accepts only ShuffleTestCase.class as input!");
    }
    this.testCase = (ShuffleTestCase) testCase;
    this.spec = spec;
    this.getShuffling = getShuffling;
  }

  public Optional<String> run() {
    List<ValidatorIndex> initialValidatorIndices =
        IntStream.range(0, testCase.getCount())
            .mapToObj(ValidatorIndex::new)
            .collect(Collectors.toList());
    List<ValidatorIndex> expectedIndices =
        testCase.getShuffled().stream().map(ValidatorIndex::new).collect(Collectors.toList());
    List<ValidatorIndex> validatorIndices =
        getShuffling.apply(
            new Triplet<>(
                initialValidatorIndices,
                Hash32.fromHexString(testCase.getSeed()),
                testCase.getCount()));
    return assertLists(expectedIndices, validatorIndices);
  }
}
