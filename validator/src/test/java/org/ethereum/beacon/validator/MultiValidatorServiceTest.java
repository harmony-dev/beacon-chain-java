package org.ethereum.beacon.validator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.Collections;
import java.util.Random;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.util.ObservableBeaconStateTestUtil;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.util.MessageSignerTestUtil;
import org.ethereum.beacon.validator.util.ValidatorServiceTestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;

public class MultiValidatorServiceTest {

  @Test
  public void recentStateIsKept() {
    Random random = new Random();
    BeaconChainSpec spec = Mockito.spy(BeaconChainSpec.createWithDefaults());

    MultiValidatorService validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, spec);
    Mockito.doReturn(true).when(spec).is_current_slot(any(), anyLong());
    Mockito.doNothing().when(validator).runTasks(any());

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState currentSlotState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec, currentSlot);

    // state was kept
    validator.onNewState(currentSlotState);
    Assert.assertEquals(currentSlotState, validator.getRecentState());

    ObservableBeaconState updatedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec, currentSlot);

    // state was updated
    validator.onNewState(updatedState);
    Assert.assertEquals(updatedState, validator.getRecentState());
  }

  @Test
  public void outboundRecentStateIsIgnored() {
    Random random = new Random();
    BeaconChainSpec spec = Mockito.spy(BeaconChainSpec.createWithDefaults());

    MultiValidatorService validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, spec);
    Mockito.doNothing().when(validator).runTasks(any());

    ObservableBeaconState outdatedState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec, SlotNumber.ZERO);

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState currentSlotState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec, currentSlot);

    Mockito.doReturn(false)
        .when(spec)
        .is_current_slot(any(), anyLong());

    // state wasn't kept
    validator.onNewState(outdatedState);
    Assert.assertNull(validator.getRecentState());

    Mockito.doReturn(true)
        .when(spec)
        .is_current_slot(any(), anyLong());

    // state was kept
    validator.onNewState(currentSlotState);
    Assert.assertEquals(currentSlotState, validator.getRecentState());

    Mockito.doReturn(false)
        .when(spec)
        .is_current_slot(any(), anyLong());

    // state wasn't updated
    validator.onNewState(outdatedState);
    Assert.assertEquals(currentSlotState, validator.getRecentState());
  }

  @Test
  public void initService() {
    Random random = new Random();
    BeaconChainSpec spec = Mockito.spy(BeaconChainSpec.createWithDefaults());

    BLSPubkey pubkey = BLSPubkey.wrap(Bytes48.random(random));
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BLS381Credentials blsCredentials = new BLS381Credentials(pubkey, signer);
    MultiValidatorService validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, spec, blsCredentials);

    ValidatorIndex validatorIndex = ValidatorIndex.of(Math.abs(random.nextInt()) % 10 + 10);
    Mockito.verify(validator, Mockito.never()).runTasks(any());

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState currentSlotState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec, currentSlot);

    ReadList<ValidatorIndex, ValidatorRecord> validatorRegistry =
        createRegistry(random, validatorIndex, pubkey);
    Mockito.doReturn(validatorRegistry)
        .when(currentSlotState.getLatestSlotState())
        .getValidators();

    Mockito.doReturn(true).when(spec).is_current_slot(any(), anyLong());
    Mockito.doReturn(validatorIndex).when(spec).get_validator_index_by_pubkey(any(), any());
    Mockito.doNothing().when(validator).runTasks(any());

    validator.onNewState(currentSlotState);

    // validatorIndex is set
    Assert.assertEquals(Collections.singleton(validatorIndex), validator.getValidatorIndices());
  }

  @Test
  public void runValidatorTasks() {
    Random random = new Random();
    BeaconChainSpec spec = Mockito.spy(BeaconChainSpec.createWithDefaults());

    BLSPubkey pubkey = BLSPubkey.wrap(Bytes48.random(random));
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BLS381Credentials blsCredentials = new BLS381Credentials(pubkey, signer);
    MultiValidatorService validator =
        ValidatorServiceTestUtil.mockBeaconChainValidator(random, spec, blsCredentials);

    ValidatorIndex validatorIndex = ValidatorIndex.of(Math.abs(random.nextInt()) % 10 + 10);

    SlotNumber currentSlot = SlotNumber.of(Math.abs(random.nextLong()) % 10 + 10);
    ObservableBeaconState initialState =
        ObservableBeaconStateTestUtil.createInitialState(random, spec, currentSlot);
    ObservableBeaconState updatedState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, spec, currentSlot.increment());
    ObservableBeaconState sameSlotState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, spec, currentSlot.increment());
    ObservableBeaconState nextSlotState =
        ObservableBeaconStateTestUtil.createInitialState(
            random, spec, currentSlot.increment().increment());

    ReadList<ValidatorIndex, ValidatorRecord> validatorRegistry =
        createRegistry(random, validatorIndex, pubkey);

    Mockito.doReturn(validatorRegistry)
        .when(initialState.getLatestSlotState())
        .getValidators();

    Mockito.doReturn(validatorRegistry)
        .when(updatedState.getLatestSlotState())
        .getValidators();

    Mockito.doReturn(validatorRegistry)
        .when(sameSlotState.getLatestSlotState())
        .getValidators();

    Mockito.doReturn(validatorRegistry)
        .when(nextSlotState.getLatestSlotState())
        .getValidators();

    Mockito.doReturn(true).when(spec).is_current_slot(any(), anyLong());
    Mockito.doReturn(validatorIndex).when(spec).get_validator_index_by_pubkey(any(), any());
    Mockito.doNothing().when(validator).runTasks(any());

    validator.onNewState(initialState);
    Assert.assertEquals(Collections.singleton(validatorIndex), validator.getValidatorIndices());

    // runTasks was called on a new state
    validator.onNewState(updatedState);
    Mockito.verify(validator, Mockito.times(1)).runTasks(updatedState);
    Mockito.verify(validator, Mockito.times(2)).runTasks(any());

    // runTasks was not called for a state belonging to the same slot
    validator.onNewState(sameSlotState);
    Mockito.verify(validator, Mockito.times(2)).runTasks(any());

    // runTasks was called again when a state for a new slot came
    validator.onNewState(nextSlotState);
    Mockito.verify(validator, Mockito.times(3)).runTasks(any());
  }

  private ReadList<ValidatorIndex, ValidatorRecord> createRegistry(
      Random random, ValidatorIndex validatorIndex, BLSPubkey pubkey) {
    WriteList<ValidatorIndex, ValidatorRecord> validatorRegistry =
        WriteList.create(ValidatorIndex::of);
    validatorRegistry.addAll(
        Collections.nCopies(
            validatorIndex.getIntValue(),
            new ValidatorRecord(
                BLSPubkey.wrap(Bytes48.random(random)),
                Hash32.ZERO,
                Gwei.ZERO,
                Boolean.FALSE,
                EpochNumber.ZERO,
                EpochNumber.ZERO,
                EpochNumber.ZERO,
                EpochNumber.ZERO)));
    validatorRegistry.add(
        new ValidatorRecord(
            pubkey,
            Hash32.ZERO,
            Gwei.ZERO,
            Boolean.FALSE,
            EpochNumber.ZERO,
            EpochNumber.ZERO,
            EpochNumber.ZERO,
            EpochNumber.ZERO));

    return validatorRegistry;
  }
}
