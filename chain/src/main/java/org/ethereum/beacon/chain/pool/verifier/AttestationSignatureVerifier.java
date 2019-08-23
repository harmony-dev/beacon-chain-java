package org.ethereum.beacon.chain.pool.verifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.schedulers.RunnableEx;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.uint.UInt64;

public class AttestationSignatureVerifier {

  private final Scheduler executor;
  private final BeaconChainSpec spec;

  private final SimpleProcessor<ReceivedAttestation> valid;
  private final SimpleProcessor<ReceivedAttestation> invalid;

  public AttestationSignatureVerifier(
      Schedulers schedulers, Scheduler executor, BeaconChainSpec spec) {
    this.executor = executor;
    this.spec = spec;

    this.valid = new SimpleProcessor<>(schedulers.events(), "AttestationSignatureVerifier.valid");
    this.invalid =
        new SimpleProcessor<>(schedulers.events(), "AttestationSignatureVerifier.invalid");
  }

  public Publisher<ReceivedAttestation> valid() {
    return valid;
  }

  public Publisher<ReceivedAttestation> invalid() {
    return invalid;
  }

  public void in(List<SignatureVerificationSet> batch) {
    execute(
        () -> {

          // validate signature encoding format
          List<SignatureVerificationSet> valid = new ArrayList<>();
          for (SignatureVerificationSet set : batch) {
            if (BLS381.Signature.validate(set.getAttestation().getMessage().getSignature())) {
              valid.add(set);
            } else {
              invalid.onNext(set.getAttestation());
            }
          }

          Map<AttestationData, List<SignatureVerificationSet>> groupedBySignedMessage =
              valid.stream()
                  .collect(
                      Collectors.groupingBy(data -> data.getAttestation().getMessage().getData()));

          groupedBySignedMessage.values().forEach(this::process);
        });
  }

  private void process(List<SignatureVerificationSet> attestations) {
    final UInt64 domain = attestations.get(0).getDomain();
    final AttestationData data = attestations.get(0).getAttestation().getMessage().getData();

    // for aggregation sake, smaller aggregates should go first
    attestations.sort(
        Comparator.comparing(set -> set.getAttestation().getMessage().getAggregationBits().size()));

    // try to aggregate as much as we can
    List<SignatureVerificationSet> aggregates = new ArrayList<>();
    List<SignatureVerificationSet> nonAggregates = new ArrayList<>();
    AggregateVerifier verifier =
        new AggregateVerifier(spec.getConstants().getMaxValidatorsPerCommittee().getIntValue());
    for (SignatureVerificationSet set : attestations) {
      if (verifier.add(set)) {
        aggregates.add(set);
      } else {
        nonAggregates.add(set);
      }
    }

    // verify aggregate and fall back to one-by-one verification if it has failed
    if (verifier.verify(spec, data, domain)) {
      aggregates.forEach(set -> valid.onNext(set.getAttestation()));
    } else {
      nonAggregates = attestations;
    }

    for (SignatureVerificationSet set : nonAggregates) {
      if (verify(spec, set, domain)) {
        valid.onNext(set.getAttestation());
      } else {
        invalid.onNext(set.getAttestation());
      }
    }
  }

  private boolean verify(BeaconChainSpec spec, SignatureVerificationSet set, UInt64 domain) {
    AttestationData data = set.getAttestation().getMessage().getData();
    Hash32 bit0Hash = spec.hash_tree_root(new AttestationDataAndCustodyBit(data, false));
    Hash32 bit1Hash = spec.hash_tree_root(new AttestationDataAndCustodyBit(data, true));

    return BLS381.verifyMultiple(
        Arrays.asList(
            MessageParameters.create(bit0Hash, domain), MessageParameters.create(bit1Hash, domain)),
        Signature.createWithoutValidation(set.getAttestation().getMessage().getSignature()),
        Arrays.asList(set.getBit0AggregateKey(), set.getBit1AggregateKey()));
  }

  private static final class AggregateVerifier {
    List<PublicKey> bit0AggregateKeys = new ArrayList<>();
    List<PublicKey> bit1AggregateKeys = new ArrayList<>();
    List<BLS381.Signature> signatures = new ArrayList<>();
    Bitlist bits;

    AggregateVerifier(int maxValidatorsPerCommittee) {
      this.bits = Bitlist.of(maxValidatorsPerCommittee);
    }

    boolean add(SignatureVerificationSet set) {
      if (!bits.and(set.getAttestation().getMessage().getAggregationBits()).isEmpty()) {
        return false;
      }

      bit0AggregateKeys.add(set.getBit0AggregateKey());
      bit1AggregateKeys.add(set.getBit1AggregateKey());
      signatures.add(
          BLS381.Signature.createWithoutValidation(
              set.getAttestation().getMessage().getSignature()));

      return true;
    }

    boolean verify(BeaconChainSpec spec, AttestationData data, UInt64 domain) {
      PublicKey bit0Key = PublicKey.aggregate(bit0AggregateKeys);
      PublicKey bit1Key = PublicKey.aggregate(bit1AggregateKeys);
      Signature signature = Signature.aggregate(signatures);

      Hash32 bit0Hash = spec.hash_tree_root(new AttestationDataAndCustodyBit(data, false));
      Hash32 bit1Hash = spec.hash_tree_root(new AttestationDataAndCustodyBit(data, true));

      return BLS381.verifyMultiple(
          Arrays.asList(
              MessageParameters.create(bit0Hash, domain),
              MessageParameters.create(bit1Hash, domain)),
          signature,
          Arrays.asList(bit0Key, bit1Key));
    }
  }

  private void execute(RunnableEx routine) {
    executor.execute(routine);
  }
}
