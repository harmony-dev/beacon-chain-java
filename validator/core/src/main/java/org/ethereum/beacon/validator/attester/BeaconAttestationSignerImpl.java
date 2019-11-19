package org.ethereum.beacon.validator.attester;

import static org.ethereum.beacon.core.spec.SignatureDomains.ATTESTATION;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.validator.BeaconAttestationSigner;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Default implementation of attestation signer.
 *
 * @see BeaconAttestationSigner
 */
public class BeaconAttestationSignerImpl implements BeaconAttestationSigner {

  private final BeaconChainSpec spec;
  private final MessageSigner<BLSSignature> signer;

  public BeaconAttestationSignerImpl(BeaconChainSpec spec, MessageSigner<BLSSignature> signer) {
    this.spec = spec;
    this.signer = signer;
  }

  @Override
  public Attestation sign(Attestation attestation, BeaconState state) {
    Hash32 hash = spec.hash_tree_root(attestation.getData());
    UInt64 domain = spec.get_domain(state, ATTESTATION, attestation.getData().getTarget().getEpoch());
    BLSSignature signature = signer.sign(hash, domain);

    return new Attestation(
        attestation.getAggregationBits(),
        attestation.getData(),
        signature,
        spec.getConstants());
  }
}
