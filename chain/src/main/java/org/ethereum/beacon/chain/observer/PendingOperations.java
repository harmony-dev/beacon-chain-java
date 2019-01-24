package org.ethereum.beacon.chain.observer;

import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;

/** A pending state interface. */
public interface PendingOperations {

  List<Attestation> getAttestations();

  List<ProposerSlashing> peekProposerSlashings(int count);

  List<CasperSlashing> peekCasperSlashings(int count);

  List<Attestation> peekAggregatedAttestations(int count);

  List<Exit> peekExits(int count);
}
