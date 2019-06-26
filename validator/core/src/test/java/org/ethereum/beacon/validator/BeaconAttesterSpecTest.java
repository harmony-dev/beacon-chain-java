package org.ethereum.beacon.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
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
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

import java.util.List;
import java.util.Random;

import static org.ethereum.beacon.validator.ValidatorSpecTestUtil.getCommittee;
import static org.mockito.ArgumentMatchers.any;

public class BeaconAttesterSpecTest {

  @Test
  public void attestASlot() {
    Random random = new Random();

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();

    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    ObservableBeaconState initiallyObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec);

    List<ValidatorIndex> committee =
        getCommittee(spec.getConstants().getTargetCommitteeSize().getIntValue());
    int indexIntoCommittee = Math.abs(random.nextInt() % committee.size());
    ValidatorIndex validatorIndex = committee.get(indexIntoCommittee);
    Hash32 targetRoot = Hash32.random(random);
    Hash32 sourceRoot = Hash32.random(random);
    ShardNumber shard =
        ShardNumber.of(UInt64.random(random).modulo(spec.getConstants().getShardCount()));
    BeaconAttesterSpec attesterSpec = getAttesterSpecMock(spec, committee, targetRoot, sourceRoot);


    BeaconStateEx stateEx = initiallyObservedState.getLatestSlotState();
    Attestation unsignedAttestation =
        attesterSpec.prepareAttestation(
            validatorIndex, shard, initiallyObservedState, stateEx.getSlot());
    BLSSignature aggregateSignature =
        attesterSpec.getAggregateSignature(
            stateEx, unsignedAttestation.getData(), signer);

    Attestation attestation =
        new Attestation(
            unsignedAttestation.getAggregationBitfield(),
            unsignedAttestation.getData(),
            unsignedAttestation.getCustodyBitfield(),
            aggregateSignature);

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

    BLSSignature expectedSignature =
        signer.sign(
            spec.hash_tree_root(new AttestationDataAndCustodyBit(data, false)),
            spec.get_domain(state, SignatureDomains.ATTESTATION));

    Assert.assertEquals(expectedSignature, attestation.getSignature());
  }

  public static BeaconAttesterSpec getAttesterSpecMock(BeaconChainSpec spec, List<ValidatorIndex> committee, Hash32 targetRoot, Hash32 sourceRoot) {
    BeaconAttesterSpec attesterSpec = Mockito.spy(new BeaconAttesterSpec(spec));

    Mockito.doReturn(committee).when(attesterSpec).getCommittee(any(), any());
    Mockito.doReturn(targetRoot).when(attesterSpec).getTargetRoot(any(), any());
    Mockito.doReturn(sourceRoot).when(attesterSpec).getSourceRoot(any(), any());

    return attesterSpec;
  }
}
