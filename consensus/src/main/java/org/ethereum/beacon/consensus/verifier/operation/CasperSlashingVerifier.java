package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.createdFailed;

import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import tech.pegasys.artemis.util.uint.UInt24;

/**
 * Verifies {@link CasperSlashing} beacon chain operation.
 *
 * @see CasperSlashing
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#casper-slashings-1">Casper
 *     slashings</a> in the spec.
 */
public class CasperSlashingVerifier implements OperationVerifier<CasperSlashing> {

  private SpecHelpers specHelpers;

  public CasperSlashingVerifier(SpecHelpers specHelpers) {
    this.specHelpers = specHelpers;
  }

  @Override
  public VerificationResult verify(CasperSlashing operation, BeaconState state) {
    SlashableVoteData slashableVoteData1 = operation.getSlashableVoteData1();
    SlashableVoteData slashableVoteData2 = operation.getSlashableVoteData2();

    if (slashableVoteData1.getData().equals(slashableVoteData2.getData())) {
      return createdFailed("slashable_vote_data_1 != slashable_vote_data_2");
    }

    List<UInt24> indices1 =
        specHelpers.indices(
            slashableVoteData1.getCustodyBit0Indices(), slashableVoteData1.getCustodyBit1Indices());
    List<UInt24> indices2 =
        specHelpers.indices(
            slashableVoteData2.getCustodyBit0Indices(), slashableVoteData2.getCustodyBit1Indices());

    List<UInt24> intersection = specHelpers.intersection(indices1, indices2);

    if (intersection.size() < 1) {
      return createdFailed("there is no intersection between indices of slashable_vote_data");
    }

    if (!(specHelpers.is_double_vote(slashableVoteData1.getData(), slashableVoteData2.getData())
        || specHelpers.is_surround_vote(
            slashableVoteData1.getData(), slashableVoteData2.getData()))) {
      return createdFailed("no slashing conditions found");
    }

    for (UInt24 index : slashableVoteData1.getCustodyBit0Indices()) {
      if (safeInt(index) >= state.getValidatorRegistryUnsafe().size()) {
        return createdFailed(
            "validator index %s is out of range, registry size %d",
            index, state.getValidatorRegistryUnsafe().size());
      }
    }

    VerificationResult result = verifyIndexes(slashableVoteData1, state);
    if (result != PASSED) {
      return result;
    }
    result = verifyIndexes(slashableVoteData2, state);
    if (result != PASSED) {
      return result;
    }

    if (!specHelpers.verify_slashable_vote_data(state, slashableVoteData1)) {
      return createdFailed("slashable_vote_data_1 is incorrect");
    }

    if (!specHelpers.verify_slashable_vote_data(state, slashableVoteData2)) {
      return createdFailed("slashable_vote_data_2 is incorrect");
    }

    return PASSED;
  }

  private VerificationResult verifyIndexes(SlashableVoteData slashableVoteData, BeaconState state) {
    VerificationResult result;
    if (PASSED != (result = verifyIndexesImpl(slashableVoteData.getCustodyBit0Indices(), state))) {
      return createdFailed("%s, custody_bit_0_indices", result.getMessage());
    }
    if (PASSED != (result = verifyIndexesImpl(slashableVoteData.getCustodyBit1Indices(), state))) {
      return createdFailed("%s, custody_bit_1_indices", result.getMessage());
    }
    return PASSED;
  }

  private VerificationResult verifyIndexesImpl(UInt24[] custodyBitIndices, BeaconState state) {
    for (int i = 0; i < custodyBitIndices.length; i++) {
      UInt24 index = custodyBitIndices[i];
      if (safeInt(index) >= state.getValidatorRegistryUnsafe().size()) {
        return createdFailed(
            "validator index %s is out of range, registry size %d, index in array: %d",
            index, state.getValidatorRegistryUnsafe().size(), i);
      }
    }
    return PASSED;
  }
}
