package org.ethereum.beacon.chain.processor;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;

public class AttestationPoolImpl implements AttestationPool {

  private static final EpochNumber HISTORIC_EPOCHS = EpochNumber.of(1);

  private final BeaconChainSpec spec;
  private final TreeMap<EpochNumber, Set<Attestation>> epochs = new TreeMap<>();

  private EpochNumber currentEpoch;

  public AttestationPoolImpl(BeaconChainSpec spec, EpochNumber currentEpoch) {
    this.spec = spec;
    this.currentEpoch = currentEpoch;
  }

  @Override
  public void onTick(SlotNumber slot) {
    EpochNumber newEpoch = spec.compute_epoch_at_slot(slot);
    if (newEpoch.greater(currentEpoch)) {
      currentEpoch = newEpoch;
      EpochNumber threshold = historyThreshold(currentEpoch);
      epochs.keySet().removeAll(epochs.headMap(threshold).keySet());
    }
  }

  @Override
  public void onAttestation(Attestation attestation) {
    EpochNumber threshold = historyThreshold(currentEpoch);
    EpochNumber targetEpoch = attestation.getData().getTarget().getEpoch();
    if (targetEpoch.greaterEqual(threshold)) {
      Set<Attestation> attestations = epochs.computeIfAbsent(targetEpoch, epoch -> new HashSet<>());
      attestations.add(attestation);
    }
  }

  @Override
  public List<Attestation> getOffChainAttestations(BeaconState state) {
    Set<Attestation> attestationChurn = new HashSet<>();
    epochs.values().forEach(attestationChurn::addAll);

    Set<PendingAttestation> onChainAttestations = new HashSet<>();
    onChainAttestations.addAll(state.getPreviousEpochAttestations().listCopy());
    onChainAttestations.addAll(state.getCurrentEpochAttestations().listCopy());

    return attestationChurn.stream()

        // sort out on chain attestations
        .filter(
            attestation -> {
              BitSet offChainBits =
                  onChainAttestations.stream()
                      .filter(pendingAtt -> pendingAtt.getData().equals(attestation.getData()))
                      .map(pendingAtt -> pendingAtt.getAggregationBits().toBitSet())
                      .reduce(
                          new BitSet(),
                          (s1, s2) -> {
                            BitSet res = new BitSet();
                            res.or(s1);
                            res.or(s2);
                            return res;
                          });

              return !offChainBits.intersects(attestation.getAggregationBits().toBitSet());
            })

        // sort out attestations not applicable to provided state
        .filter(attestation -> spec.verify_attestation(state, attestation))
        .collect(Collectors.toList());
  }

  private EpochNumber historyThreshold(EpochNumber currentEpoch) {
    if (currentEpoch.less(HISTORIC_EPOCHS)) {
      return EpochNumber.ZERO;
    }
    return currentEpoch.minus(HISTORIC_EPOCHS);
  }
}
