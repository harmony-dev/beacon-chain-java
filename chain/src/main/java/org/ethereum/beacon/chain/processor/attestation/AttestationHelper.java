package org.ethereum.beacon.chain.processor.attestation;

import java.util.BitSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.PendingAttestation;
import tech.pegasys.artemis.util.collections.Bitlist;

public abstract class AttestationHelper {
  private AttestationHelper() {}

  public static BitSet getOnChainBits(BeaconState state, AttestationData data) {
    BitSet previousBits =
        computeAggregationBits(
            StreamSupport.stream(state.getPreviousEpochAttestations().spliterator(), false)
                .filter(pendingAttestation -> pendingAttestation.getData().equals(data))
                .map(PendingAttestation::getAggregationBits));
    BitSet currentBits =
        computeAggregationBits(
            StreamSupport.stream(state.getPreviousEpochAttestations().spliterator(), false)
                .filter(pendingAttestation -> pendingAttestation.getData().equals(data))
                .map(PendingAttestation::getAggregationBits));

    BitSet result = BitSet.valueOf(previousBits.toLongArray());
    result.or(currentBits);
    return result;
  }

  public static BitSet computeAggregationBits(Stream<Bitlist> attestationStream) {
    return attestationStream
        .map(Bitlist::toBitSet)
        .reduce(
            new BitSet(),
            (s1, s2) -> {
              BitSet res = BitSet.valueOf(s1.toLongArray());
              res.or(s2);
              return res;
            });
  }
}
