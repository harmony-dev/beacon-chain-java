package org.ethereum.beacon.consensus.transition;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ValidatorIndex;

public class EpochTransitionSummary {

  public class EpochSummary {
    Gwei validatorBalance;
    Gwei boundaryAttestingBalance;

    List<ValidatorIndex> activeAttesters = new ArrayList<>();
    List<ValidatorIndex> boundaryAttesters = new ArrayList<>();

    public Gwei getValidatorBalance() {
      return validatorBalance;
    }

    public Gwei getBoundaryAttestingBalance() {
      return boundaryAttestingBalance;
    }

    public List<ValidatorIndex> getActiveAttesters() {
      return activeAttesters;
    }

    public List<ValidatorIndex> getBoundaryAttesters() {
      return boundaryAttesters;
    }
  }

  BeaconStateEx preState;
  BeaconStateEx postState;

  EpochSummary previousEpochSummary = new EpochSummary();
  EpochSummary currentEpochSummary = new EpochSummary();

  Gwei headAttestingBalance;
  Gwei justifiedAttestingBalance;
  List<ValidatorIndex> headAttesters = new ArrayList<>();
  List<ValidatorIndex> justifiedAttesters = new ArrayList<>();

  boolean noFinality;
  Gwei[][] attestationDeltas = { new Gwei[0], new Gwei[0] };

  List<ValidatorIndex> ejectedValidators = new ArrayList<>();

  public BeaconStateEx getPreState() {
    return preState;
  }

  public BeaconStateEx getPostState() {
    return postState;
  }

  public EpochSummary getPreviousEpochSummary() {
    return previousEpochSummary;
  }

  public EpochSummary getCurrentEpochSummary() {
    return currentEpochSummary;
  }

  public Gwei getHeadAttestingBalance() {
    return headAttestingBalance;
  }

  public Gwei getJustifiedAttestingBalance() {
    return justifiedAttestingBalance;
  }

  public List<ValidatorIndex> getHeadAttesters() {
    return headAttesters;
  }

  public List<ValidatorIndex> getJustifiedAttesters() {
    return justifiedAttesters;
  }

  public boolean isNoFinality() {
    return noFinality;
  }

  public Gwei[][] getAttestationDeltas() {
    return attestationDeltas;
  }
  
  public List<ValidatorIndex> getEjectedValidators() {
    return ejectedValidators;
  }
}
