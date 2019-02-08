package org.ethereum.beacon.chain.observer;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.types.BLSPubkey;
import tech.pegasys.artemis.util.uint.UInt64;

/** A pending state interface. */
public interface PendingOperations {

  List<Attestation> getAttestations();

  Optional<Attestation> findAttestation(BLSPubkey pubKey);

  List<ProposerSlashing> peekProposerSlashings(int maxCount);

  List<AttesterSlashing> peekAttesterSlashings(int maxCount);

  List<Attestation> peekAggregatedAttestations(int maxCount, UInt64 maxSlot);

  List<Exit> peekExits(int maxCount);
}
