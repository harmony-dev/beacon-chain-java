package org.ethereum.beacon.test;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.StateTransitions;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.runner.state.StateComparator;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.state.SanityBlocksCase;
import org.ethereum.beacon.test.type.state.field.PostField;
import org.ethereum.beacon.test.type.state.field.PreField;
import org.javatuples.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Tests of cases found during Interop event. Check <A
 * href="https://github.com/djrtwo/interop-test-cases">https://github.com/djrtwo/interop-test-cases</A>
 * for more details.
 */
@Ignore("It's been merged into a main suite https://github.com/ethereum/eth2.0-specs/pull/1411")
public class InteropTests extends TestUtils {

  /**
   * See <a
   * href="https://github.com/djrtwo/interop-test-cases/blob/master/tests/artemis_16_crosslinks_and_balances/notes.md">https://github.com/djrtwo/interop-test-cases/blob/master/tests/artemis_16_crosslinks_and_balances/notes.md</a>
   * for details.
   *
   * <p>Actually block processing state test with auto slot/epoch transitions
   */
  @Test
  public void testArtemisConsensusBreak() {
    runSpecTestsInResourceDir(
        Paths.get("interop-tests"),
        Paths.get("artemis-break00"),
        SanityBlocksCase.class,
        input -> {
          BrokeBlockRunner testRunner = new BrokeBlockRunner(input.getValue0(), input.getValue1());
          return testRunner.run();
        });
  }

  public class BrokeBlockRunner implements Runner {
    private TestCase testCase;
    private BeaconChainSpec spec;

    public BrokeBlockRunner(TestCase testCase, BeaconChainSpec spec) {
      this.testCase = testCase;
      this.spec = spec;
    }

    public Optional<String> run() {
      if (!(testCase instanceof PreField)) {
        throw new RuntimeException("TestCase runner accepts only test cases with Pre field");
      }
      BeaconState latestState = ((PreField) testCase).getPre(spec.getConstants());
      Optional<String> processingError;

      BeaconState stateBackup = latestState.createMutableCopy();
      if (testCase instanceof SanityBlocksCase) {
        Pair<Optional<String>, BeaconState> processingBlocks =
            processBlocks(
                ((SanityBlocksCase) testCase).getBlocks(spec.getConstants()), latestState);
        processingError = processingBlocks.getValue0();
        if (!processingError.isPresent()) {
          latestState = processingBlocks.getValue1();
        }
      } else {
        throw new RuntimeException("This type of state test is not supported");
      }

      if (processingError.isPresent()) {
        latestState = stateBackup;
      }
      if (!(testCase instanceof PostField)) {
        throw new RuntimeException("TestCase runner accepts only test cases with Post field");
      }
      if (((PostField) testCase).getPost(spec.getConstants()) == null) { // XXX: Not changed
        return StateComparator.compare(
            ((PreField) testCase).getPre(spec.getConstants()), latestState, spec);
      } else {
        Optional compareResult =
            StateComparator.compare(
                ((PostField) testCase).getPost(spec.getConstants()), latestState, spec);
        if (!compareResult.isPresent()) {
          return Optional.empty();
        }

        String processingErrorMessage = "Processing error: ";
        if (processingError.isPresent()) {
          processingErrorMessage += processingError.get();
        }
        return Optional.of(compareResult.get() + processingErrorMessage);
      }
    }

    private Pair<Optional<String>, BeaconState> processBlocks(
        List<SignedBeaconBlock> blocks, BeaconState state) {
      EmptySlotTransition preBlockTransition = StateTransitions.preBlockTransition(spec);
      PerBlockTransition blockTransition = StateTransitions.blockTransition(spec);
      BeaconBlockVerifier blockVerifier = BeaconBlockVerifier.createDefault(spec);
      BeaconStateVerifier stateVerifier = BeaconStateVerifier.createDefault(spec);
      BeaconStateEx stateEx = new BeaconStateExImpl(state);
      try {
        for (SignedBeaconBlock block : blocks) {
          stateEx = preBlockTransition.apply(stateEx, block.getMessage().getSlot());
          VerificationResult blockVerification = blockVerifier.verify(block, stateEx);
          if (!blockVerification.isPassed()) {
            return Pair.with(Optional.of("Invalid block"), null);
          }
          stateEx = blockTransition.apply(stateEx, block.getMessage());
          // XXX: incorrect block in case, with wrong consensus, modifying state root
          BeaconBlock blockWFixedRoot =
              new BeaconBlock(
                  block.getMessage().getSlot(),
                  block.getMessage().getParentRoot(),
                  spec.hash_tree_root(((PostField) testCase).getPost(spec.getConstants())),
                  block.getMessage().getBody()
                  );
          VerificationResult stateVerification = stateVerifier.verify(stateEx, blockWFixedRoot);
          if (!stateVerification.isPassed()) {
            return Pair.with(Optional.of("State mismatch"), null);
          }
        }
        return Pair.with(Optional.empty(), stateEx.createMutableCopy());
      } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
        return Pair.with(Optional.of(ex.getMessage()), null);
      }
    }
  }
}
