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
import org.ethereum.beacon.core.operations.attestation.AttestationDataAndCustodyBit;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.spec.SignatureDomains;
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

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(SpecConstants.DEFAULT);

    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BeaconChainAttesterImpl attester = BeaconChainAttesterTestUtil.mockAttester(specHelpers);
    ObservableBeaconState initiallyObservedState =
        ObservableBeaconStateTestUtil.createInitialState(random, specHelpers);

    List<ValidatorIndex> committee =
        getCommittee(specHelpers.getConstants().getTargetCommitteeSize().getIntValue());
    int indexIntoCommittee = Math.abs(random.nextInt() % committee.size());
    ValidatorIndex validatorIndex = committee.get(indexIntoCommittee);
    Hash32 epochBoundaryRoot = Hash32.random(random);
    Hash32 justifiedBlockRoot = Hash32.random(random);
    ShardNumber shard = specHelpers.getConstants().getBeaconChainShardNumber();

    Mockito.doReturn(committee).when(attester).getCommittee(any(), any());
    Mockito.doReturn(epochBoundaryRoot).when(attester).getEpochBoundaryRoot(any(), any());
    Mockito.doReturn(Crosslink.EMPTY).when(attester).getLatestCrosslink(any(), any());
    Mockito.doReturn(justifiedBlockRoot).when(attester).getJustifiedBlockRoot(any());

    Attestation attestation =
        attester.attest(validatorIndex, shard, initiallyObservedState, signer);

    AttestationData data = attestation.getData();
    BeaconState state = initiallyObservedState.getLatestSlotState();

    Assert.assertEquals(state.getSlot(), data.getSlot());
    Assert.assertEquals(shard, data.getShard());
    Assert.assertEquals(
        specHelpers.hash_tree_root(initiallyObservedState.getHead()), data.getBeaconBlockRoot());
    Assert.assertEquals(epochBoundaryRoot, data.getEpochBoundaryRoot());
    Assert.assertEquals(Hash32.ZERO, data.getCrosslinkDataRoot());
    Assert.assertEquals(Hash32.ZERO, data.getLatestCrosslink().getCrosslinkDataRoot());
    Assert.assertEquals(state.getJustifiedEpoch(), data.getJustifiedEpoch());
    Assert.assertEquals(justifiedBlockRoot, data.getJustifiedBlockRoot());

    int bitfieldSize = (committee.size() - 1) / 8 + 1;

    Assert.assertEquals(bitfieldSize, attestation.getAggregationBitfield().size());
    Assert.assertEquals(bitfieldSize, attestation.getCustodyBitfield().size());

    Assert.assertTrue(attestation.getCustodyBitfield().isZero());

    byte aByte = attestation.getAggregationBitfield().get(indexIntoCommittee / 8);
    Assert.assertEquals(1, ((aByte & 0xFF) >>> (indexIntoCommittee % 8)));

    BLSSignature expectedSignature =
        signer.sign(
            specHelpers.hash_tree_root(new AttestationDataAndCustodyBit(data, false)),
            specHelpers.get_domain(
                state.getForkData(),
                specHelpers.get_current_epoch(state),
                SignatureDomains.ATTESTATION));

    Assert.assertEquals(expectedSignature, attestation.getAggregateSignature());
  }

  private List<ValidatorIndex> getCommittee(int size) {
    return IntStream.range(0, size).mapToObj(ValidatorIndex::of).collect(Collectors.toList());
  }
}
