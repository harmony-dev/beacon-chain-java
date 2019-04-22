package org.ethereum.beacon.test.runner.shuffle;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.shuffle.ShuffleTestCase;
import org.ethereum.beacon.test.type.TestCase;
import org.javatuples.Triplet;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.ethereum.beacon.test.SilentAsserts.assertLists;

/** TestRunner for {@link ShuffleTestCase} */
public class ShuffleRunner implements Runner {
  private ShuffleTestCase testCase;
  private BeaconChainSpec spec;
  private Function<
          Triplet<Hash32, ReadList<ValidatorIndex, ValidatorRecord>, EpochNumber>,
          List<List<ValidatorIndex>>>
      getShuffling;

  public ShuffleRunner(
      TestCase testCase,
      BeaconChainSpec spec,
      Function<
              Triplet<Hash32, ReadList<ValidatorIndex, ValidatorRecord>, EpochNumber>,
              List<List<ValidatorIndex>>>
          getShuffling) {
    if (!(testCase instanceof ShuffleTestCase)) {
      throw new RuntimeException("TestCase runner accepts only ShuffleTestCase.class as input!");
    }
    this.testCase = (ShuffleTestCase) testCase;
    this.spec = spec;
    this.getShuffling = getShuffling;
  }

  public Optional<String> run() {

    EpochNumber currentEpoch = EpochNumber.castFrom(UInt64.valueOf(testCase.getInput().getEpoch()));
    List<ValidatorRecord> validators = new ArrayList<>();
    for (ShuffleTestCase.ShuffleInput.ShuffleTestValidator testValidator :
        testCase.getInput().getValidators()) {
      ValidatorRecord validatorRecord =
          new ValidatorRecord(
              BLSPubkey.ZERO,
              Hash32.ZERO,
              EpochNumber.castFrom(UInt64.valueOf(testValidator.getActivation_epoch())),
              EpochNumber.castFrom(UInt64.valueOf(testValidator.getExit_epoch())),
              EpochNumber.castFrom(UInt64.valueOf(testValidator.getActivation_epoch()))
                  .plus(spec.getConstants().getMinValidatorWithdrawabilityDelay()),
              false, // XXX: not used
              false);
      validators.add(validatorRecord);
    }

    ReadList<ValidatorIndex, ValidatorRecord> validatorReadList =
        ReadList.wrap(validators, ValidatorIndex::of);

    List<List<ValidatorIndex>> expectedIndices = new ArrayList<>();
    for (List<Integer> entry : testCase.getOutput()) {
      List<ValidatorIndex> indices = new ArrayList<>();
      for (Integer index : entry) {
        indices.add(ValidatorIndex.of(index));
      }
      expectedIndices.add(indices);
    }
    List<List<ValidatorIndex>> validatorIndices =
        getShuffling.apply(
            new Triplet<>(
                Hash32.fromHexString(testCase.getSeed()), validatorReadList, currentEpoch));
    return assertLists(expectedIndices, validatorIndices);
  }
}
