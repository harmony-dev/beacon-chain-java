package org.ethereum.beacon.chain.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.mockito.Mockito;

public class PendingOperationsTestUtil {

  public static PendingOperations createEmptyPendingOperations() {
    return mockPendingOperations(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());
  }

  public static PendingOperations mockPendingOperations(
      List<Attestation> attestations,
      List<ProposerSlashing> proposerSlashings,
      List<CasperSlashing> casperSlashings,
      List<Exit> exits) {
    PendingOperations pendingOperations = Mockito.mock(PendingOperations.class);
    when(pendingOperations.getAttestations()).thenReturn(attestations);
    when(pendingOperations.peekProposerSlashings(anyInt())).thenReturn(proposerSlashings);
    when(pendingOperations.peekCasperSlashings(anyInt())).thenReturn(casperSlashings);
    when(pendingOperations.peekAggregatedAttestations(anyInt(), any())).thenReturn(attestations);
    when(pendingOperations.peekExits(anyInt())).thenReturn(exits);
    return pendingOperations;
  }
}
