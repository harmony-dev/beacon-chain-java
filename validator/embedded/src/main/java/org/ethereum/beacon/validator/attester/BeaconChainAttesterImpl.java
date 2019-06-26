package org.ethereum.beacon.validator.attester;

import com.google.common.annotations.VisibleForTesting;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconAttesterSpec;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.ValidatorService;
import org.ethereum.beacon.validator.crypto.MessageSigner;

/**
 * An implementation of beacon chain attester.
 *
 * @see BeaconChainAttester
 * @see ValidatorService
 */
public class BeaconChainAttesterImpl implements BeaconChainAttester {

  /** Attester routines */
  private final BeaconAttesterSpec beaconAttesterSpec;

  public BeaconChainAttesterImpl(BeaconChainSpec spec) {
    this.beaconAttesterSpec = new BeaconAttesterSpec(spec);
  }

  @Override
  public Attestation attest(
      ValidatorIndex validatorIndex,
      ShardNumber shard,
      ObservableBeaconState observableState,
      MessageSigner<BLSSignature> signer) {
    BeaconState state = observableState.getLatestSlotState();
    Attestation attestation =
        getBeaconAttesterSpec().prepareAttestation(
            validatorIndex, shard, observableState, state.getSlot());
    BLSSignature aggregateSignature = getAggregateSignature(state, attestation.getData(), signer);

    return new Attestation(
        attestation.getAggregationBitfield(),
        attestation.getData(),
        attestation.getCustodyBitfield(),
        aggregateSignature);
  }

  /**
   * Creates {@link AttestationDataAndCustodyBit} instance and signs off on it.
   *
   * @param state a state at a slot that validator is attesting in.
   * @param data an attestation data instance.
   * @param signer message signer.
   * @return signature of attestation data and custody bit.
   */
  private BLSSignature getAggregateSignature(
      BeaconState state, AttestationData data, MessageSigner<BLSSignature> signer) {
    return getBeaconAttesterSpec().getAggregateSignature(state, data, signer);
  }

  @VisibleForTesting
  BeaconAttesterSpec getBeaconAttesterSpec() {
    return beaconAttesterSpec;
  }
}
