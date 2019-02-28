package org.ethereum.beacon.consensus.transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  Map<ValidatorIndex, Gwei> attestationRewards = new HashMap<>();
  Map<ValidatorIndex, Gwei> attestationPenalties = new HashMap<>();
  Map<ValidatorIndex, Gwei> boundaryAttestationRewards = new HashMap<>();
  Map<ValidatorIndex, Gwei> boundaryAttestationPenalties = new HashMap<>();
  Map<ValidatorIndex, Gwei> beaconHeadAttestationRewards = new HashMap<>();
  Map<ValidatorIndex, Gwei> beaconHeadAttestationPenalties = new HashMap<>();
  Map<ValidatorIndex, Gwei> inclusionDistanceRewards = new HashMap<>();
  Map<ValidatorIndex, Gwei> penalizedEpochPenalties = new HashMap<>();
  Map<ValidatorIndex, Gwei> noFinalityPenalties = new HashMap<>();
  Map<ValidatorIndex, Gwei> attestationInclusionRewards = new HashMap<>();

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

  public Map<ValidatorIndex, Gwei> getAttestationRewards() {
    return attestationRewards;
  }

  public Map<ValidatorIndex, Gwei> getAttestationPenalties() {
    return attestationPenalties;
  }

  public Map<ValidatorIndex, Gwei> getBoundaryAttestationRewards() {
    return boundaryAttestationRewards;
  }

  public Map<ValidatorIndex, Gwei> getBoundaryAttestationPenalties() {
    return boundaryAttestationPenalties;
  }

  public Map<ValidatorIndex, Gwei> getBeaconHeadAttestationRewards() {
    return beaconHeadAttestationRewards;
  }

  public Map<ValidatorIndex, Gwei> getBeaconHeadAttestationPenalties() {
    return beaconHeadAttestationPenalties;
  }

  public Map<ValidatorIndex, Gwei> getInclusionDistanceRewards() {
    return inclusionDistanceRewards;
  }

  public Map<ValidatorIndex, Gwei> getPenalizedEpochPenalties() {
    return penalizedEpochPenalties;
  }

  public Map<ValidatorIndex, Gwei> getNoFinalityPenalties() {
    return noFinalityPenalties;
  }

  public Map<ValidatorIndex, Gwei> getAttestationInclusionRewards() {
    return attestationInclusionRewards;
  }

  public List<ValidatorIndex> getEjectedValidators() {
    return ejectedValidators;
  }
}
