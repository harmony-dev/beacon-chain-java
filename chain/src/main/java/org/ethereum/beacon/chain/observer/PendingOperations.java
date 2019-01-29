package org.ethereum.beacon.chain.observer;

import java.util.List;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import tech.pegasys.artemis.util.uint.UInt64;

/** A pending state interface. */
public interface PendingOperations {

  List<Attestation> getAttestations();

  List<ProposerSlashing> peekProposerSlashings(int maxCount);

  List<CasperSlashing> peekCasperSlashings(int maxCount);

  List<Attestation> peekAggregatedAttestations(int maxCount, UInt64 maxSlot);

  List<Exit> peekExits(int maxCount);
}
