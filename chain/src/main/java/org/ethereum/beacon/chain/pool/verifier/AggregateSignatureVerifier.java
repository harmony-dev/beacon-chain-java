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
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * An implementation of aggregate-then-verify strategy.
 *
 * <p>In a few words this strategy looks as follows:
 *
 * <ol>
 *   <li>Aggregate as much attestation as we can.
 *   <li>Verify aggregate signature.
 *   <li>Verify signatures of attestations that aren't aggregated in a one-by-one fashion.
 * </ol>
 *
 * <p>If second step fails verification falls back to one-by-one strategy.
 */
public class AggregateSignatureVerifier {

  /** A beacon chain spec. */
  private final BeaconChainSpec spec;
  /** A domain which attestation signatures has been created with. */
  private final UInt64 domain;
  /** Verification churn. */
  private final List<VerifiableAttestation> attestations = new ArrayList<>();

  AggregateSignatureVerifier(BeaconChainSpec spec, UInt64 domain) {
    this.spec = spec;
    this.domain = domain;
  }

  /**
   * Adds an attestation to the verification churn.
   *
   * @param state a state attestation built upon.
   * @param indexed an instance of corresponding {@link IndexedAttestation}.
   * @param attestation an attestation itself.
   */
  void add(BeaconState state, IndexedAttestation indexed, ReceivedAttestation attestation) {
    attestations.add(VerifiableAttestation.create(spec, state, indexed, attestation));
  }

  /**
   * Verifies previously added attestation.
   *
   * <p>First, attestations are grouped by {@link AttestationData} and then each group is passed
   * onto {@link #verifyGroup(AttestationData, List)}.
   *
   * @return a result of singature verification.
   */
  public VerificationResult verify() {
    Map<AttestationData, List<VerifiableAttestation>> signedMessageGroups =
        attestations.stream().collect(Collectors.groupingBy(VerifiableAttestation::getData));

    return signedMessageGroups.entrySet().stream()
        .map(e -> verifyGroup(e.getKey(), e.getValue()))
        .reduce(VerificationResult.EMPTY, VerificationResult::merge);
  }

  /**
   * Verifies a group of attestations signing the same {@link AttestationData}.
   *
   * @param data attestation data.
   * @param group a group.
   * @return a result of verification.
   */
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
          attestation.getAggregationBits(),
          attestation.getBit0Key(),
          attestation.getBit1Key(),
          attestation.getSignature())) {
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
      if (verifySignature(
          data, attestation.getBit0Key(), attestation.getBit1Key(), attestation.getSignature())) {
        valid.add(attestation.getAttestation());
      } else {
        invalid.add(attestation.getAttestation());
      }
    }

    return new VerificationResult(valid, invalid);
  }

  /**
   * Verifies single signature.
   *
   * @param data a data being signed.
   * @param bit0Key a bit0 public key.
   * @param bit1Key a bit1 public key.
   * @param signature a signature.
   * @return {@code true} if signature is valid, {@code false} otherwise.
   */
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
}
