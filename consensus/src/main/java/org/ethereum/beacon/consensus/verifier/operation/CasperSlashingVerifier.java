package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.SpecHelpers.safeInt;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.core.types.ValidatorIndex;
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
  public VerificationResult verify(CasperSlashing casperSlashing, BeaconState state) {
    SlashableVoteData slashableVoteData1 = casperSlashing.getSlashableVoteData1();
    SlashableVoteData slashableVoteData2 = casperSlashing.getSlashableVoteData2();

    specHelpers.checkIndexRange(state, slashableVoteData1.getCustodyBit0Indices());
    specHelpers.checkIndexRange(state, slashableVoteData1.getCustodyBit1Indices());
    specHelpers.checkIndexRange(state, slashableVoteData2.getCustodyBit0Indices());
    specHelpers.checkIndexRange(state, slashableVoteData2.getCustodyBit1Indices());
    specHelpers.checkShardRange(slashableVoteData1.getData().getShard());
    specHelpers.checkShardRange(slashableVoteData2.getData().getShard());

    if (slashableVoteData1.getData().equals(slashableVoteData2.getData())) {
      return failedResult("slashable_vote_data_1 != slashable_vote_data_2");
    }

    List<ValidatorIndex> intersection = specHelpers.custodyIndexIntersection(casperSlashing);

    if (intersection.size() < 1) {
      return failedResult("there is no intersection between indices of slashable_vote_data");
    }

    if (!(specHelpers.is_double_vote(slashableVoteData1.getData(), slashableVoteData2.getData())
        || specHelpers.is_surround_vote(
            slashableVoteData1.getData(), slashableVoteData2.getData()))) {
      return failedResult("no slashing conditions found");
    }

    for (ValidatorIndex index : slashableVoteData1.getCustodyBit0Indices()) {
      if (index.greaterEqual(state.getValidatorRegistry().size())) {
        return failedResult(
            "validator index %s is out of range, registry size %d",
            index, state.getValidatorRegistry().size());
      }
    }

    if (!specHelpers.verify_slashable_vote_data(state, slashableVoteData1)) {
      return failedResult("slashable_vote_data_1 is incorrect");
    }

    if (!specHelpers.verify_slashable_vote_data(state, slashableVoteData2)) {
      return failedResult("slashable_vote_data_2 is incorrect");
    }

    return PASSED;
  }
}
