package org.ethereum.beacon.validator.attester;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.util.MessageSignerTestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconChainAttesterTest {

  @Test
  public void attestASlot() {
    Random random = new Random();

    SpecHelpers specHelpers = new SpecHelpers(ChainSpec.DEFAULT);

    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BeaconChainAttesterImpl attester = BeaconChainAttesterTestUtil.mockAttester(specHelpers);
    ObservableBeaconState initiallyObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers);

    List<ValidatorIndex> committee =
        getCommittee(specHelpers.getChainSpec().getTargetCommitteeSize().getIntValue());
    int indexIntoCommittee = Math.abs(random.nextInt() % committee.size());
    ValidatorIndex validatorIndex = committee.get(indexIntoCommittee);
    Hash32 epochBoundaryRoot = Hash32.random(random);
    Hash32 latestCrosslinkRoot = Hash32.random(random);
    Hash32 justifiedBlockRoot = Hash32.random(random);
    ShardNumber shard = specHelpers.getChainSpec().getBeaconChainShardNumber();

    Mockito.doReturn(committee).when(attester).getCommittee(any(), any());
    Mockito.doReturn(epochBoundaryRoot).when(attester).getEpochBoundaryRoot(any(), any());
    Mockito.doReturn(latestCrosslinkRoot).when(attester).getLatestCrosslinkRoot(any(), any());
    Mockito.doReturn(justifiedBlockRoot).when(attester).getJustifiedBlockRoot(any(), any());

    Attestation attestation =
        attester.attest(validatorIndex, shard, initiallyObservedState, signer);

    AttestationData data = attestation.getData();
    BeaconState state = initiallyObservedState.getLatestSlotState();

    Assert.assertEquals(state.getSlot(), data.getSlot());
    Assert.assertEquals(shard, data.getShard());
    Assert.assertEquals(
        specHelpers.hash_tree_root(initiallyObservedState.getHead()), data.getBeaconBlockRoot());
    Assert.assertEquals(epochBoundaryRoot, data.getEpochBoundaryRoot());
    Assert.assertEquals(Hash32.ZERO, data.getShardBlockRoot());
    Assert.assertEquals(latestCrosslinkRoot, data.getLatestCrosslinkRoot());
    Assert.assertEquals(state.getJustifiedSlot(), data.getJustifiedSlot());
    Assert.assertEquals(justifiedBlockRoot, data.getJustifiedBlockRoot());

    int bitfieldSize = (committee.size() - 1) / 8 + 1;

    Assert.assertEquals(bitfieldSize, attestation.getParticipationBitfield().size());
    Assert.assertEquals(bitfieldSize, attestation.getCustodyBitfield().size());

    Assert.assertTrue(attestation.getCustodyBitfield().isZero());

    byte aByte = attestation.getParticipationBitfield().get(indexIntoCommittee / 8);
    Assert.assertEquals(1, (aByte >> (indexIntoCommittee % 8)) & 0xFF);
  }

  private List<ValidatorIndex> getCommittee(int size) {
    return IntStream.range(0, size).mapToObj(ValidatorIndex::of).collect(Collectors.toList());
  }
}
