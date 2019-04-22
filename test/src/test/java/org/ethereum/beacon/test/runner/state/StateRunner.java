package org.ethereum.beacon.test.runner.state;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.StateTransitions;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.chainspec.SpecDataUtils;
import org.ethereum.beacon.emulator.config.chainspec.SpecHelpersData;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.runner.state.StateComparator;
import org.ethereum.beacon.test.type.state.StateTestCase;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.util.Objects;
import org.javatuples.Pair;

/** TestRunner for {@link StateTestCase} */
public class StateRunner implements Runner {
  private StateTestCase testCase;

  public StateRunner(TestCase testCase) {
    if (!(testCase instanceof StateTestCase)) {
      throw new RuntimeException("TestCase runner accepts only StateTestCase.class as input!");
    }
    this.testCase = (StateTestCase) testCase;
  }

  public Optional<String> run() {
    BeaconChainSpec spec;
    try {
      spec = buildSpec(testCase);
    } catch (Exception e) {
      return Optional.of("Failed to build BeaconChainSpec: " + e.getMessage());
    }

    BeaconStateEx initialState = buildInitialState(spec, testCase.getInitialState());
    Optional<String> err = StateComparator.compare(testCase.getInitialState(), initialState);
    if (err.isPresent()) {
      return Optional.of("Initial state parsed incorrectly: " + err.get());
    }

    EmptySlotTransition preBlockTransition = StateTransitions.preBlockTransition(spec);
    BlockTransition<BeaconStateEx> blockTransition = StateTransitions.blockTransition(spec);

    BeaconStateEx latestState = initialState;
    for (StateTestCase.BlockData blockData : testCase.getBlocks()) {
      Pair<BeaconBlock, Optional<String>> blockPair = StateTestUtils.parseBlockData(blockData);
      if (blockPair.getValue1().isPresent()) {
        return blockPair.getValue1();
      }
      BeaconBlock block = blockPair.getValue0();

      BeaconStateEx postBlockState;
      try {
        BeaconStateEx preBlockState = preBlockTransition.apply(latestState, block.getSlot());
        postBlockState = blockTransition.apply(preBlockState, block);
      } catch (Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return Optional.of("Error happened during transition: " + sw.toString());
      }
      latestState = postBlockState;
    }

    return StateComparator.compare(testCase.getExpectedState(), latestState);
  }

  private BeaconStateEx buildInitialState(BeaconChainSpec spec, BeaconStateData stateData) {
    BeaconState state = StateTestUtils.parseBeaconState(spec.getConstants(), stateData);
    return new BeaconStateExImpl(state, TransitionType.BLOCK);
  }

  private BeaconChainSpec buildSpec(StateTestCase testCase)
      throws InvocationTargetException, IllegalAccessException {
    SpecConstantsData specConstantsData =
        Objects.copyProperties(
            SpecDataUtils.createSpecConstantsData(BeaconChainSpec.DEFAULT_CONSTANTS),
            testCase.getConfig());

    SpecHelpersData specHelpersData = new SpecHelpersData();
    specHelpersData.setBlsVerify(testCase.getVerifySignatures());

    SpecData specData = new SpecData();
    specData.setSpecHelpersOptions(specHelpersData);
    specData.setSpecConstants(specConstantsData);

    return new SpecBuilder().withSpec(specData).buildSpec();
  }
}
