package org.ethereum.beacon.chain.pool.verifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.uint.UInt64;

public class AggregateSignatureVerifier implements SignatureVerifier {
  private final BeaconChainSpec spec;
  private final UInt64 domain;
  private final List<VerifiableAttestation> attestations = new ArrayList<>();

  public AggregateSignatureVerifier(BeaconChainSpec spec, UInt64 domain) {
    this.spec = spec;
    this.domain = domain;
  }

  @Override
  public void feed(BeaconState state, IndexedAttestation indexed, ReceivedAttestation attestation) {
    attestations.add(VerifiableAttestation.create(spec, state, indexed, attestation));
  }

  @Override
  public VerificationResult verify() {
    Map<AttestationData, List<VerifiableAttestation>> signedMessageGroups =
        attestations.stream().collect(Collectors.groupingBy(VerifiableAttestation::getData));

    return signedMessageGroups.entrySet().stream()
        .map(e -> verifyGroup(e.getKey(), e.getValue()))
        .reduce(VerificationResult.EMPTY, VerificationResult::merge);
  }

  private VerificationResult verifyGroup(AttestationData data, List<VerifiableAttestation> group) {
    final List<ReceivedAttestation> valid = new ArrayList<>();
    final List<ReceivedAttestation> invalid = new ArrayList<>();

    // for aggregation sake, smaller aggregates should go first
    group.sort(Comparator.comparing(attestation -> attestation.getAggregationBits().size()));

    // try to aggregate as much as we can
    List<VerifiableAttestation> aggregated = new ArrayList<>();
    List<VerifiableAttestation> notAggregated = new ArrayList<>();
    AggregateSignature aggregate = new AggregateSignature();
    for (VerifiableAttestation attestation : group) {
      if (aggregate.add(
          attestation.aggregationBits,
          attestation.bit0Key,
          attestation.bit1Key,
          attestation.signature)) {
        aggregated.add(attestation);
      } else {
        notAggregated.add(attestation);
      }
    }

    // verify aggregate and fall back to one-by-one verification if it has failed
    if (verifySignature(data, aggregate.getKey0(), aggregate.getKey1(), aggregate.getSignature())) {
      aggregated.stream().map(VerifiableAttestation::getAttestation).forEach(valid::add);
    } else {
      notAggregated = group;
    }

    for (VerifiableAttestation attestation : notAggregated) {
      if (verifySignature(data, attestation.bit0Key, attestation.bit1Key, attestation.signature)) {
        valid.add(attestation.attestation);
      } else {
        invalid.add(attestation.attestation);
      }
    }

    return new VerificationResult(valid, invalid);
  }

  private boolean verifySignature(
      AttestationData data, PublicKey bit0Key, PublicKey bit1Key, Signature signature) {
    Hash32 bit0Hash = spec.hash_tree_root(new AttestationDataAndCustodyBit(data, false));
    Hash32 bit1Hash = spec.hash_tree_root(new AttestationDataAndCustodyBit(data, true));

    return BLS381.verifyMultiple(
        Arrays.asList(
            MessageParameters.create(bit0Hash, domain), MessageParameters.create(bit1Hash, domain)),
        signature,
        Arrays.asList(bit0Key, bit1Key));
  }

  private static final class AggregateSignature {
    private Bitlist bits;
    private List<PublicKey> key0s = new ArrayList<>();
    private List<PublicKey> key1s = new ArrayList<>();
    private List<BLS381.Signature> sigs = new ArrayList<>();

    boolean add(Bitlist bits, PublicKey key0, PublicKey key1, BLS381.Signature sig) {
      // if bits has intersection it's not possible to get a viable aggregate
      if (this.bits != null && !this.bits.and(bits).isEmpty()) {
        return false;
      }

      if (this.bits == null) {
        this.bits = bits;
      } else {
        this.bits = this.bits.or(bits);
      }

      key0s.add(key0);
      key1s.add(key1);
      sigs.add(sig);

      return true;
    }

    PublicKey getKey0() {
      return PublicKey.aggregate(key0s);
    }

    PublicKey getKey1() {
      return PublicKey.aggregate(key1s);
    }

    Signature getSignature() {
      return Signature.aggregate(sigs);
    }
  }

  private static final class VerifiableAttestation {

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
          attestation.getMessage().getData(),
          attestation.getMessage().getAggregationBits(),
          bit0AggregateKey,
          bit1AggregateKey,
          BLS381.Signature.createWithoutValidation(attestation.getMessage().getSignature()));
    }

    private final ReceivedAttestation attestation;

    private final AttestationData data;
    private final Bitlist aggregationBits;
    private final PublicKey bit0Key;
    private final PublicKey bit1Key;
    private final BLS381.Signature signature;

    public VerifiableAttestation(
        ReceivedAttestation attestation,
        AttestationData data,
        Bitlist aggregationBits,
        PublicKey bit0Key,
        PublicKey bit1Key,
        Signature signature) {
      this.attestation = attestation;
      this.data = data;
      this.aggregationBits = aggregationBits;
      this.bit0Key = bit0Key;
      this.bit1Key = bit1Key;
      this.signature = signature;
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
  }
}
