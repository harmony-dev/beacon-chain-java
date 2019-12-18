package org.ethereum.beacon.chain.observer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;

public class NaivePendingOperations implements PendingOperations {

  private final List<Attestation> attestations;

  public NaivePendingOperations(List<Attestation> attestations) {
    this.attestations = attestations;
  }

  @Override
  public List<Attestation> getAttestations() {
    return attestations;
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
  public List<Attestation> peekAggregateAttestations(int maxCount, SpecConstants specConstants) {
    return attestations.stream().limit(maxCount).collect(Collectors.toList());
  }

  @Override
  public List<VoluntaryExit> peekExits(int maxCount) {
    return Collections.emptyList();
  }
}
