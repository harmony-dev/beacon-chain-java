package org.ethereum.beacon.chain.observer;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.crypto.BLS381;

public class PendingOperationsState implements PendingOperations {

  Map<BLSPubkey, Attestation> attestations;

  public PendingOperationsState(Map<BLSPubkey, Attestation> attestations) {
    this.attestations = attestations;
  }

  @Override
  public Optional<Attestation> findAttestation(BLSPubkey pubKey) {
    return Optional.ofNullable(attestations.get(pubKey));
  }

  @Override
  public List<Attestation> getAttestations() {
    return new ArrayList<>(attestations.values());
  }

  @Override
  public List<ProposerSlashing> peekProposerSlashings(int maxCount) {
    return Collections.emptyList();
  }

  @Override
  public List<AttesterSlashing> peekAttesterSlashings(int maxCount) {
    return Collections.emptyList();
  }

  @Override
  public List<Attestation> peekAggregatedAttestations(int maxCount, SlotNumber maxSlot) {
    Map<SlotNumber, List<Attestation>> attestationsBySlot =
        getAttestations().stream()
            .filter(attestation -> attestation.getData().getSlot().lessEqual(maxSlot))
            .collect(groupingBy(at -> at.getData().getSlot()));
    return attestationsBySlot.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .limit(maxCount)
        .map(entry -> aggregateAttestations(entry.getValue()))
        .collect(Collectors.toList());
  }

  private Attestation aggregateAttestations(List<Attestation> attestations) {
    assert !attestations.isEmpty();

    Bitfield participants =
        attestations.stream()
            .map(Attestation::getAggregationBitfield)
            .reduce(Bitfield::and)
            .get();
    Bitfield custody =
        attestations.stream().map(Attestation::getCustodyBitfield).reduce(Bitfield::and).get();
    BLS381.Signature aggregatedSignature =
        BLS381.Signature.aggregate(
            attestations.stream()
                .map(Attestation::getAggregateSignature)
                .map(BLS381.Signature::create)
                .collect(Collectors.toList()));
    BLSSignature aggSign = BLSSignature.wrap(aggregatedSignature.getEncoded());

    return new Attestation(
        attestations.get(0).getData(), participants, custody, aggSign);
  }

  @Override
  public List<Exit> peekExits(int maxCount) {
    return Collections.emptyList();
  }
}
