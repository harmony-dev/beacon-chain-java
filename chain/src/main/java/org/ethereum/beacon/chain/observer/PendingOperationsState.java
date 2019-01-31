package org.ethereum.beacon.chain.observer;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.types.Bitfield;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class PendingOperationsState implements PendingOperations {

  Map<Bytes48, Attestation> attestations;

  public PendingOperationsState(Map<Bytes48, Attestation> attestations) {
    this.attestations = attestations;
  }

  @Override
  public Optional<Attestation> findAttestation(Bytes48 pubKey) {
    return Optional.of(attestations.get(pubKey));
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
  public List<CasperSlashing> peekCasperSlashings(int maxCount) {
    return Collections.emptyList();
  }

  @Override
  public List<Attestation> peekAggregatedAttestations(int maxCount, UInt64 maxSlot) {
    Map<UInt64, List<Attestation>> attestationsBySlot =
        getAttestations().stream()
            .filter(attestation -> attestation.getData().getSlot().compareTo(maxSlot) <= 0)
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
            .map(Attestation::getParticipationBitfield)
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

    return new Attestation(
        attestations.get(0).getData(), participants, custody, aggregatedSignature.getEncoded());
  }

  @Override
  public List<Exit> peekExits(int maxCount) {
    return Collections.emptyList();
  }
}
