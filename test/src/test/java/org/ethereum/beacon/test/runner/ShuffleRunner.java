package org.ethereum.beacon.test.runner;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.type.ShuffleTestCase;
import org.ethereum.beacon.test.type.TestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;

/**
 * TestRunner for {@link org.ethereum.beacon.test.type.ShuffleTestCase}
 */
public class ShuffleRunner implements Runner {
  ShuffleTestCase testCase;
  private SpecHelpers specHelpers;

  public ShuffleRunner(TestCase testCase, SpecHelpers specHelpers) {
    if (!(testCase instanceof ShuffleTestCase)) {
      throw new RuntimeException("TestCase runner accepts only ShuffleTestCase.class as input!");
    }
    this.testCase = (ShuffleTestCase) testCase;
    this.specHelpers = specHelpers;
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
                  .plus(specHelpers.getConstants().getMinValidatorWithdrawabilityDelay()),
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
        specHelpers.get_shuffling(
            Hash32.fromHexString(testCase.getSeed()), validatorReadList, currentEpoch);

    try {
      assertArrayEquals(expectedIndices.toArray(), validatorIndices.toArray());
    } catch (AssertionError e) {
      return Optional.of(e.getMessage());
    }

    return Optional.empty();
  }
}
