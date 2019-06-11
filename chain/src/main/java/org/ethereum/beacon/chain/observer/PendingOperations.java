package org.ethereum.beacon.chain.observer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.SlotNumber;

/** A pending state interface. */
public interface PendingOperations {

  static PendingOperations getEmpty() {
    return new PendingOperations() {
      @Override
      public List<Attestation> getAttestations() {
        return Collections.emptyList();
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
      public List<Attestation> peekAggregateAttestations(int maxCount) {
        return Collections.emptyList();
      }

      @Override
      public List<VoluntaryExit> peekExits(int maxCount) {
        return Collections.emptyList();
      }

      @Override
      public List<Transfer> peekTransfers(int maxCount) {
        return Collections.emptyList();
      }
    };
  }

  List<Attestation> getAttestations();

  List<ProposerSlashing> peekProposerSlashings(int maxCount);

  List<AttesterSlashing> peekAttesterSlashings(int maxCount);

  List<Attestation> peekAggregateAttestations(int maxCount);

  List<VoluntaryExit> peekExits(int maxCount);

  List<Transfer> peekTransfers(int maxCount);

  default String toStringShort() {
    return "PendingOperations["
        + (getAttestations().isEmpty() ? "" : "attest: " + getAttestations().size())
        + "]";
  }

  default String toStringMedium(SpecConstants spec) {
    String ret = "PendingOperations[";
    if (!getAttestations().isEmpty()) {
      ret += "attest (slot/shard/beaconBlock): [";
      for (Attestation att : getAttestations()) {
        ret += att.toStringShort(spec) + ", ";
      }
      ret = ret.substring(0, ret.length() - 2);
    }
    ret += "]";
    return ret;
  }
}
