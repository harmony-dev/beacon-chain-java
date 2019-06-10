package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * An interface of beacon chain attester. A part of beacon validator logic.
 *
 * @see ValidatorService
 */
public interface BeaconChainAttester {

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
}
