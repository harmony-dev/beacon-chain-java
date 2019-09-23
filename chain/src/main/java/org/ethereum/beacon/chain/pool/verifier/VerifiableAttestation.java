package org.ethereum.beacon.chain.pool.verifier;

import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import tech.pegasys.artemis.util.collections.Bitlist;

/**
 * An artificial entity exclusively related to signature verification process.
 *
 * <p>Being created from {@link IndexedAttestation}, {@link BeaconState} and {@link Attestation}
 * itself contains all the information required to proceed with signature verification:
 *
 * <p>
 *
 * <ul>
 *   <li>bit0 and bit1 aggregate public keys
 *   <li>signature
 *   <li>aggregation bits
 *   <li>attestation data
 * </ul>
 */
final class VerifiableAttestation {

  static VerifiableAttestation create(
      BeaconChainSpec spec,
      BeaconState state,
      IndexedAttestation indexed,
      ReceivedAttestation attestation) {

    List<BLSPubkey> bit0Keys =
        indexed.getCustodyBit0Indices().stream()
            .map(i -> state.getValidators().get(i).getPubKey())
            .collect(Collectors.toList());
    List<BLSPubkey> bit1Keys =
        indexed.getCustodyBit1Indices().stream()
            .map(i -> state.getValidators().get(i).getPubKey())
            .collect(Collectors.toList());

    // pre-process aggregated pubkeys
    PublicKey bit0AggregateKey = spec.bls_aggregate_pubkeys_no_validate(bit0Keys);
    PublicKey bit1AggregateKey = spec.bls_aggregate_pubkeys_no_validate(bit1Keys);

    return new VerifiableAttestation(
        attestation,
        indexed,
        attestation.getMessage().getData(),
        attestation.getMessage().getAggregationBits(),
        bit0AggregateKey,
        bit1AggregateKey,
        BLS381.Signature.createWithoutValidation(attestation.getMessage().getSignature()));
  }

  private final ReceivedAttestation attestation;
  private final IndexedAttestation indexed;

  private final AttestationData data;
  private final Bitlist aggregationBits;
  private final PublicKey bit0Key;
  private final PublicKey bit1Key;
  private final BLS381.Signature signature;

  public VerifiableAttestation(
      ReceivedAttestation attestation,
      IndexedAttestation indexed,
      AttestationData data,
      Bitlist aggregationBits,
      PublicKey bit0Key,
      PublicKey bit1Key,
      Signature signature) {
    this.attestation = attestation;
    this.indexed = indexed;
    this.data = data;
    this.aggregationBits = aggregationBits;
    this.bit0Key = bit0Key;
    this.bit1Key = bit1Key;
    this.signature = signature;
  }

  public IndexedAttestation getIndexed() {
    return indexed;
  }

  public AttestationData getData() {
    return data;
  }

  public Bitlist getAggregationBits() {
    return aggregationBits;
  }

  public ReceivedAttestation getAttestation() {
    return attestation;
  }

  public PublicKey getBit0Key() {
    return bit0Key;
  }

  public PublicKey getBit1Key() {
    return bit1Key;
  }

  public Signature getSignature() {
    return signature;
  }
}
