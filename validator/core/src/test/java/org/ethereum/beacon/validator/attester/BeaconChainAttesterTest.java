package org.ethereum.beacon.validator.attester;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconAttestationSigner;
import org.ethereum.beacon.validator.MessageSignerTestUtil;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;

public class BeaconChainAttesterTest {

  @Test
  public void attestASlot() {
    Random random = new Random();

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();

    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BeaconChainAttesterImpl attester = BeaconChainAttesterTestUtil.mockAttester(spec);
    ObservableBeaconState initiallyObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec);

    List<ValidatorIndex> committee =
        getCommittee(spec.getConstants().getTargetCommitteeSize().getIntValue());
    int indexIntoCommittee = Math.abs(random.nextInt() % committee.size());
    ValidatorIndex validatorIndex = committee.get(indexIntoCommittee);
    Hash32 targetRoot = Hash32.random(random);
    Hash32 sourceRoot = Hash32.random(random);
    CommitteeIndex committeeIndex =
        new CommitteeIndex(UInt64.random(random).modulo(spec.getConstants().getMaxCommitteesPerSlot()
            .times(spec.getConstants().getSlotsPerEpoch())));

    BeaconState state = initiallyObservedState.getLatestSlotState();
    Mockito.doReturn(committee).when(attester).getCommittee(any(), any());
    Mockito.doReturn(new Checkpoint(spec.get_current_epoch(state), targetRoot))
        .when(attester)
        .getTarget(any(), any(), any());
    Mockito.doReturn(new Checkpoint(state.getCurrentJustifiedCheckpoint().getEpoch(), sourceRoot))
        .when(attester)
        .getSource(any());

    Attestation attestation =
        attester.attest(
            validatorIndex,
            committeeIndex,
            initiallyObservedState.getLatestSlotState(),
            initiallyObservedState.getHead());

    AttestationData data = attestation.getData();

    Assert.assertEquals(spec.get_current_epoch(state), data.getTarget().getEpoch());
    Assert.assertEquals(committeeIndex, data.getIndex());
    Assert.assertEquals(
        spec.signing_root(initiallyObservedState.getHead()), data.getBeaconBlockRoot());
    Assert.assertEquals(
        new Checkpoint(spec.get_current_epoch(state), targetRoot), data.getTarget());

    Assert.assertEquals(
        new Checkpoint(state.getCurrentJustifiedCheckpoint().getEpoch(), sourceRoot),
        data.getSource());

    int bitfieldSize = committee.size();

    Assert.assertEquals(bitfieldSize, attestation.getAggregationBits().size());

    byte aByte = attestation.getAggregationBits().get(indexIntoCommittee / 8);
    Assert.assertEquals(1, ((aByte & 0xFF) >>> (indexIntoCommittee % 8)));

    Attestation signedAttestation =
        BeaconAttestationSigner.getInstance(spec, signer).sign(attestation, state);

    BLSSignature expectedSignature =
        signer.sign(
            spec.hash_tree_root(data),
            spec.get_domain(state, SignatureDomains.BEACON_ATTESTER));

    Assert.assertEquals(expectedSignature, signedAttestation.getSignature());
  }

  private List<ValidatorIndex> getCommittee(int size) {
    return IntStream.range(0, size).mapToObj(ValidatorIndex::of).collect(Collectors.toList());
  }
}
