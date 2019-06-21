package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

/**
 * An interface of beacon chain attester. A part of beacon validator logic.
 *
 * @see ValidatorService
 */
public interface BeaconChainAttester {

  /**
   * Creates {@link AttestationDataAndCustodyBit} instance and signs off on it.
   *
   * @param state a state at a slot that validator is attesting in.
   * @param data an attestation data instance.
   * @param signer message signer.
   * @param spec Beacon chain spec
   * @return signature of attestation data and custody bit.
   */
  static BLSSignature getAggregateSignature(
      BeaconState state,
      AttestationData data,
      MessageSigner<BLSSignature> signer,
      BeaconChainSpec spec) {
    AttestationDataAndCustodyBit attestationDataAndCustodyBit =
        new AttestationDataAndCustodyBit(data, false);
    Hash32 hash = spec.hash_tree_root(attestationDataAndCustodyBit);
    UInt64 domain = spec.get_domain(state, ATTESTATION);
    return signer.sign(hash, domain);
  }

  /**
   * Creates an attestation to a head block at a slot given state is related to.
   *
   * @param validatorIndex index of the validator.
   * @param shard shard number.
   * @param observableState a state that attestation is based on.
   * @param signer an instance that signs off on {@link AttestationDataAndCustodyBit}.
   * @return created attestation.
   */
  Attestation attest(
      ValidatorIndex validatorIndex,
      ShardNumber shard,
      ObservableBeaconState observableState,
      MessageSigner<BLSSignature> signer);

  /**
   * Main part of {@link #attest(ValidatorIndex, ShardNumber, ObservableBeaconState, MessageSigner)}
   * logic Prepares attestation with signature stubbed with zero BLS Signature Later signer could
   * easily sign it
   *
   * @param validatorIndex index of the validator.
   * @param shard shard number.
   * @param observableState a state that attestation is based on.
   * @param slot attestation slot
   * @return unsigned attestation
   */
  Attestation prepareAttestation(
      ValidatorIndex validatorIndex,
      ShardNumber shard,
      ObservableBeaconState observableState,
      SlotNumber slot);
}
