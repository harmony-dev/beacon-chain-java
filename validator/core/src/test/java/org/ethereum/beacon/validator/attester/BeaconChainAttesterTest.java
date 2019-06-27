package org.ethereum.beacon.validator.attester;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.BeaconAttestationSigner;
import org.ethereum.beacon.validator.MessageSignerTestUtil;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

public class BeaconChainAttesterTest {

  @Test
  public void attestASlot() {
    Random random = new Random();

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();

    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BeaconChainAttesterImpl attester = BeaconChainAttesterTestUtil.mockAttester(spec);
    BeaconAttestationSigner attestationSigner = new BeaconAttestationSignerImpl(spec);
    ObservableBeaconState initiallyObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec);

    List<ValidatorIndex> committee =
        getCommittee(spec.getConstants().getTargetCommitteeSize().getIntValue());
    int indexIntoCommittee = Math.abs(random.nextInt() % committee.size());
    ValidatorIndex validatorIndex = committee.get(indexIntoCommittee);
    Hash32 targetRoot = Hash32.random(random);
    Hash32 sourceRoot = Hash32.random(random);
    ShardNumber shard = ShardNumber.of(
        UInt64.random(random).modulo(spec.getConstants().getShardCount()));

    Mockito.doReturn(committee).when(attester).getCommittee(any(), any());
    Mockito.doReturn(targetRoot).when(attester).getTargetRoot(any(), any());
    Mockito.doReturn(sourceRoot).when(attester).getSourceRoot(any());

    Attestation attestation =
        attester.attest(
            validatorIndex,
            shard,
            initiallyObservedState.getLatestSlotState(),
            initiallyObservedState.getHead());

    AttestationData data = attestation.getData();
    BeaconState state = initiallyObservedState.getLatestSlotState();

    Assert.assertEquals(spec.get_current_epoch(state), data.getTargetEpoch());
    Assert.assertEquals(shard, data.getCrosslink().getShard());
    Assert.assertEquals(
        spec.signing_root(initiallyObservedState.getHead()), data.getBeaconBlockRoot());
    Assert.assertEquals(targetRoot, data.getTargetRoot());

    Hash32 dataRoot = Hash32.ZERO; // Note: This is a stub for phase 0.
    Crosslink parentCrosslink = state.getCurrentCrosslinks().get(shard);
    Hash32 parentRoot = spec.hash_tree_root(parentCrosslink);
    EpochNumber startEpoch = parentCrosslink.getEndEpoch();
    EpochNumber endEpoch =
        UInt64s.min(
            spec.slot_to_epoch(state.getSlot()),
            parentCrosslink.getEndEpoch().plus(spec.getConstants().getMaxEpochsPerCrosslink()));

    Assert.assertEquals(
        new Crosslink(shard, startEpoch, endEpoch, parentRoot, dataRoot), data.getCrosslink());
    Assert.assertEquals(state.getCurrentJustifiedEpoch(), data.getSourceEpoch());
    Assert.assertEquals(sourceRoot, data.getSourceRoot());

    int bitfieldSize = (committee.size() - 1) / 8 + 1;

    Assert.assertEquals(bitfieldSize, attestation.getAggregationBitfield().size());
    Assert.assertEquals(bitfieldSize, attestation.getCustodyBitfield().size());

    Assert.assertTrue(attestation.getCustodyBitfield().isZero());

    byte aByte = attestation.getAggregationBitfield().get(indexIntoCommittee / 8);
    Assert.assertEquals(1, ((aByte & 0xFF) >>> (indexIntoCommittee % 8)));

    Attestation signedAttestation = attestationSigner.sign(attestation, state.getFork(), signer);

    BLSSignature expectedSignature =
        signer.sign(
            spec.hash_tree_root(new AttestationDataAndCustodyBit(data, false)),
            spec.get_domain(state, SignatureDomains.ATTESTATION));

    Assert.assertEquals(expectedSignature, signedAttestation.getSignature());
  }

  private List<ValidatorIndex> getCommittee(int size) {
    return IntStream.range(0, size).mapToObj(ValidatorIndex::of).collect(Collectors.toList());
  }
}
