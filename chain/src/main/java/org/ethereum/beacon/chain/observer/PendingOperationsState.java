package org.ethereum.beacon.chain.observer;

import static java.util.stream.Collectors.groupingBy;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.crypto.BLS381;

public class PendingOperationsState implements PendingOperations {

  Map<BLSPubkey, List<Attestation>> attestations;

  public PendingOperationsState(Map<BLSPubkey, List<Attestation>> attestations) {
    this.attestations = attestations;
  }

  @Override
  public Optional<Attestation> getLatestAttestation(BLSPubkey pubKey) {
    return Optional.ofNullable(attestations.get(pubKey))
        .map(atts -> Collections.max(atts, Comparator.comparing(att -> att.getData().getSlot())));
  }

  @Override
  public List<Attestation> getAttestations() {
    return attestations.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
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
  public List<Attestation> peekAggregatedAttestations(
      int maxCount, SlotNumber minSlotExclusive, SlotNumber maxSlotInclusive) {

    Map<AttestationData, List<Attestation>> attestationsBySlot =
        getAttestations()
            .stream()
            .filter(attestation -> attestation.getData().getSlot().greater(minSlotExclusive))
            .filter(attestation -> attestation.getData().getSlot().lessEqual(maxSlotInclusive))
            .collect(groupingBy(Attestation::getData));
    return attestationsBySlot
        .entrySet()
        .stream()
        .sorted(Comparator.comparing(e -> e.getKey().getSlot()))
        .limit(maxCount)
        .map(entry -> aggregateAttestations(entry.getValue()))
        .collect(Collectors.toList());
  }

  private Attestation aggregateAttestations(List<Attestation> attestations) {
    assert !attestations.isEmpty();
    assert attestations.stream().skip(1).allMatch(a -> a.equals(attestations.get(0)));

    Bitfield participants =
        attestations.stream()
            .map(Attestation::getAggregationBitfield)
            .reduce(Bitfield::or)
            .get();
    Bitfield custody =
        attestations.stream().map(Attestation::getCustodyBitfield).reduce(Bitfield::or).get();
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
  public List<VoluntaryExit> peekExits(int maxCount) {
    return Collections.emptyList();
  }

  @Override
  public List<Transfer> peekTransfers(int maxCount) {
    return Collections.emptyList();
  }
}
