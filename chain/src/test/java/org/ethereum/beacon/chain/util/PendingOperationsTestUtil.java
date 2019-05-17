package org.ethereum.beacon.chain.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.mockito.Mockito;

public class PendingOperationsTestUtil {

  public static PendingOperations createEmptyPendingOperations() {
    return mockPendingOperations(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  public static PendingOperations mockPendingOperations(
      List<Attestation> attestations,
      List<Attestation> aggregateAttestations,
      List<ProposerSlashing> proposerSlashings,
      List<AttesterSlashing> attesterSlashings,
      List<VoluntaryExit> voluntaryExits) {
    PendingOperations pendingOperations = Mockito.mock(PendingOperations.class);
    when(pendingOperations.getAttestations()).thenReturn(attestations);
    when(pendingOperations.peekProposerSlashings(anyInt())).thenReturn(proposerSlashings);
    when(pendingOperations.peekAttesterSlashings(anyInt())).thenReturn(attesterSlashings);
    when(pendingOperations.peekAggregateAttestations(anyInt()))
        .thenReturn(aggregateAttestations);
    when(pendingOperations.peekExits(anyInt())).thenReturn(voluntaryExits);
    return pendingOperations;
  }
}
