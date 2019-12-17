package org.ethereum.beacon.validator.proposer;

import java.util.BitSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.StreamSupport;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.collections.Bitlist;

public class ProfitEvaluator implements BiFunction<Attestation, BeaconState, Gwei> {

  private final BeaconChainSpec spec;

  public ProfitEvaluator(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public Gwei apply(Attestation attestation, BeaconState state) {
    BitSet newAggregationBits =
        StreamSupport.stream(state.getCurrentEpochAttestations().spliterator(), false)
            .filter(
                pendingAttestation -> pendingAttestation.getData().equals(attestation.getData()))
            .map(PendingAttestation::getAggregationBits)
            .reduce(Bitlist::or)
            .map(
                pendingBits -> {
                  BitSet bits1 = pendingBits.toBitSet();
                  BitSet bits2 = attestation.getAggregationBits().toBitSet();
                  bits1.xor(bits2);

                  return bits1;
                })
            .orElse(attestation.getAggregationBits().toBitSet());

    List<ValidatorIndex> beaconCommittee =
        spec.get_beacon_committee(
            state, attestation.getData().getSlot(), attestation.getData().getIndex());

    return newAggregationBits.stream()
        .boxed()
        .map(beaconCommittee::get)
        // sort out slashed validators
        .filter(index -> !state.getValidators().get(index).getSlashed())
        // calculate proposer profit for each attestation bit
        .map(
            index ->
                spec.get_base_reward(state, index)
                    .dividedBy(spec.getConstants().getProposerRewardQuotient()))
        // summarize
        .reduce(Gwei.ZERO, Gwei::plus);
  }
}
