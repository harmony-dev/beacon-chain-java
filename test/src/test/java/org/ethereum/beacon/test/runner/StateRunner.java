package org.ethereum.beacon.test.runner;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.chain.observer.PendingOperationsState;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.transition.StateCachingTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.simulator.SimulatorLauncher;
import org.ethereum.beacon.test.type.StateTestCase;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.util.Objects;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.validator.crypto.BLS381MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** TestRunner for {@link StateTestCase} */
public class StateRunner implements Runner {
  private StateTestCase testCase;
  private Function<SpecConstants, SpecHelpers> specHelpersBuilder;
  private SimulatorLauncher.MDCControlledSchedulers schedulers;

  public StateRunner(TestCase testCase, Function<SpecConstants, SpecHelpers> specHelpersBuilder) {
    if (!(testCase instanceof StateTestCase)) {
      throw new RuntimeException("TestCase runner accepts only StateTestCase.class as input!");
    }
    this.testCase = (StateTestCase) testCase;
    this.specHelpersBuilder = specHelpersBuilder;
    this.schedulers = new SimulatorLauncher.MDCControlledSchedulers();
  }

  public Optional<String> run() {
    SpecConstantsData specConstantsData = SpecConstantsData.getDefaultCopy();
    try {
      specConstantsData = Objects.copyProperties(specConstantsData, testCase.getConfig());
    } catch (Exception e) {
      return Optional.of("Failed to setup SpecConstants");
    }
    SpecConstants specConstants = SpecBuilder.buildSpecConstants(specConstantsData);
    SpecHelpers specHelpers = specHelpersBuilder.apply(specConstants);
    Time time = Time.of(testCase.getInitialState().getGenesisTime());
    List<Deposit> initialDeposits = new ArrayList<>();
    Eth1Data eth1Data = new Eth1Data(Hash32.ZERO, Hash32.ZERO);
    DepositContract.ChainStart chainStartEvent =
        new DepositContract.ChainStart(time, eth1Data, initialDeposits);
    InitialStateTransition initialTransition =
        new InitialStateTransition(chainStartEvent, specHelpers);
    PerSlotTransition perSlotTransition = new PerSlotTransition(specHelpers);
    PerBlockTransition perBlockTransition = new PerBlockTransition(specHelpers);
    PerEpochTransition perEpochTransition = new PerEpochTransition(specHelpers);
    StateCachingTransition stateCachingTransition = new StateCachingTransition(specHelpers);
    ExtendedSlotTransition extendedSlotTransition =
        new ExtendedSlotTransition(
            stateCachingTransition, perEpochTransition, perSlotTransition, specHelpers);

    InMemoryDatabase db = new InMemoryDatabase();
    BeaconChainStorageFactory storageFactory = new MemBeaconChainStorageFactory();
    BeaconChainStorage beaconChainStorage = storageFactory.create(db);

    BeaconBlockVerifier blockVerifier = BeaconBlockVerifier.createDefault(specHelpers);
    BeaconStateVerifier stateVerifier = BeaconStateVerifier.createDefault(specHelpers);

    initializeStorage(specHelpers, initialTransition, beaconChainStorage);
    BeaconTuple genesis =
        beaconChainStorage
            .getTupleStorage()
            .get(beaconChainStorage.getJustifiedStorage().get().get())
            .get();
    if (genesis
            .getState()
            .getSlot()
            .compareTo(SlotNumber.castFrom(UInt64.valueOf(testCase.getInitialState().getSlot())))
        != 0) {
      // TODO: find out how to run tests on slots which is too far from genesis
      return Optional.of("Slot transition from genesis is not implemented yet!");
    }
    MutableBeaconState state = genesis.getState().createMutableCopy();

    if (testCase.getVerifySignatures()) {
      return Optional.of(
          "Verification of signatures is required in test case but not implemented yet");
    }

    List<ValidatorRecord> validators = new ArrayList<>();
    List<BLS381Credentials> validatorCreds = new ArrayList<>();
    for (StateTestCase.BeaconStateData.ValidatorData validatorData :
        testCase.getInitialState().getValidatorRegistry()) {
      ValidatorRecord validatorRecord =
          new ValidatorRecord(
              BLSPubkey.fromHexString(validatorData.getPubkey()),
              Hash32.fromHexString(validatorData.getWithdrawalCredentials()),
              EpochNumber.castFrom(UInt64.valueOf(validatorData.getActivationEpoch())),
              EpochNumber.castFrom(UInt64.valueOf(validatorData.getExitEpoch())),
              EpochNumber.castFrom(UInt64.valueOf(validatorData.getWithdrawableEpoch())),
              validatorData.getInitiatedExit(),
              validatorData.getSlashed());
      validators.add(validatorRecord);
      BLS381Credentials validatorCred =
          new BLS381Credentials(
              BLSPubkey.fromHexString(validatorData.getPubkey()),
              (messageHash, domain) ->
                  BLSSignature.wrap(BLS381.Signature.create(new ECP2()).getEncoded()));
      validatorCreds.add(validatorCred);
    }
    state.getValidatorRegistry().addAll(validators);
    for (String balanceStr : testCase.getInitialState().getValidatorBalances()) {
      UInt64 balance = UInt64.valueOf(balanceStr);
      state.getValidatorBalances().add(Gwei.castFrom(balance));
    }

    PendingOperations pendingOperations = new PendingOperationsState(new HashMap<>());
    BeaconStateEx latestState =
        new BeaconStateExImpl(state, specHelpers.hash_tree_root(genesis.getBlock()));
    ObservableBeaconState observableBeaconState =
        new ObservableBeaconState(genesis.getBlock(), latestState, pendingOperations);
    BLS381MessageSigner signer =
        (messageHash, domain) ->
            BLSSignature.wrap(BLS381.Signature.create(new ECP2()).getEncoded());
    //    BeaconBlock block = beaconChainProposer.propose(observableBeaconState, signer);

    BeaconStateEx postBlockState = null;
    if (testCase.getBlocks().isEmpty()) {
      return Optional.of("Empty blocks size not implemented yet");
    }

    for (StateTestCase.BlockData blockData : testCase.getBlocks()) {
      Eth1Data eth1Data1 =
          new Eth1Data(
              Hash32.fromHexString(blockData.getBody().getEth1Data().getDepositRoot()),
              Hash32.fromHexString(blockData.getBody().getEth1Data().getBlockHash()));

      // Attestations
      List<Attestation> attestations = new ArrayList<>();
      for (StateTestCase.BeaconStateData.AttestationData attestationData :
          blockData.getBody().getAttestations()) {
        AttestationData attestationData1 =
            new AttestationData(
                SlotNumber.castFrom(UInt64.valueOf(attestationData.getData().getSlot())),
                Hash32.fromHexString(attestationData.getData().getBeaconBlockRoot()),
                EpochNumber.castFrom(UInt64.valueOf(attestationData.getData().getSourceEpoch())),
                Hash32.fromHexString(attestationData.getData().getSourceRoot()),
                Hash32.fromHexString(attestationData.getData().getTargetRoot()),
                ShardNumber.of(attestationData.getData().getShard()),
                new Crosslink(
                    EpochNumber.castFrom(
                        UInt64.valueOf(
                            attestationData.getData().getPreviousCrosslink().getEpoch())),
                    Hash32.fromHexString(
                        attestationData.getData().getPreviousCrosslink().getCrosslinkDataRoot())),
                Hash32.fromHexString(attestationData.getData().getCrosslinkDataRoot()));
        Attestation attestation =
            new Attestation(
                Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
                attestationData1,
                Bitfield.of(BytesValue.fromHexString(attestationData.getCustodyBitfield())),
                BLSSignature.wrap(Bytes96.fromHexString(attestationData.getAggregateSignature())));
        attestations.add(attestation);
      }

      if (!blockData.getBody().getAttesterSlashings().isEmpty()) {
        return Optional.of("Implement block attestation slashings!");
      }

      // Deposits
      List<Deposit> deposits = new ArrayList<>();
      for (StateTestCase.BlockData.BlockBodyData.DepositData depositData :
          blockData.getBody().getDeposits()) {
        Deposit deposit =
            new Deposit(
                depositData.getProof().stream()
                    .map(Hash32::fromHexString)
                    .collect(Collectors.toList()),
                UInt64.valueOf(depositData.getIndex()),
                new DepositData(
                    Gwei.castFrom(UInt64.valueOf(depositData.getDepositData().getAmount())),
                    Time.of(depositData.getDepositData().getTimestamp()),
                    new DepositInput(
                        BLSPubkey.fromHexString(
                            depositData.getDepositData().getDepositInput().getPubkey()),
                        Hash32.fromHexString(
                            depositData
                                .getDepositData()
                                .getDepositInput()
                                .getWithdrawalCredentials()),
                        BLSSignature.wrap(
                            Bytes96.fromHexString(
                                depositData
                                    .getDepositData()
                                    .getDepositInput()
                                    .getProofOfPossession())))));
        deposits.add(deposit);
      }

      // Proposer slashings
      List<ProposerSlashing> proposerSlashings = new ArrayList<>();
      for (StateTestCase.BlockData.BlockBodyData.SlashingData slashingData :
          blockData.getBody().getProposerSlashings()) {
        BeaconBlockHeader header1 =
            new BeaconBlockHeader(
                SlotNumber.castFrom(UInt64.valueOf(slashingData.getHeader1().getSlot())),
                Hash32.fromHexString(slashingData.getHeader1().getPreviousBlockRoot()),
                Hash32.fromHexString(slashingData.getHeader1().getStateRoot()),
                Hash32.fromHexString(slashingData.getHeader1().getBlockBodyRoot()),
                BLSSignature.wrap(Bytes96.fromHexString(slashingData.getHeader1().getSignature())));
        BeaconBlockHeader header2 =
            new BeaconBlockHeader(
                SlotNumber.castFrom(UInt64.valueOf(slashingData.getHeader2().getSlot())),
                Hash32.fromHexString(slashingData.getHeader2().getPreviousBlockRoot()),
                Hash32.fromHexString(slashingData.getHeader2().getStateRoot()),
                Hash32.fromHexString(slashingData.getHeader2().getBlockBodyRoot()),
                BLSSignature.wrap(Bytes96.fromHexString(slashingData.getHeader2().getSignature())));
        ProposerSlashing proposerSlashing =
            new ProposerSlashing(
                ValidatorIndex.of(slashingData.getProposerIndex()), header1, header2);
        proposerSlashings.add(proposerSlashing);
      }

      // Transfers
      List<Transfer> transfers = new ArrayList<>();
      for (StateTestCase.BlockData.BlockBodyData.TransferData transferData :
          blockData.getBody().getTransfers()) {
        Transfer transfer =
            new Transfer(
                ValidatorIndex.of(transferData.getSender()),
                ValidatorIndex.of(transferData.getRecipient()),
                Gwei.castFrom(UInt64.valueOf(transferData.getAmount())),
                Gwei.castFrom(UInt64.valueOf(transferData.getFee())),
                SlotNumber.castFrom(UInt64.valueOf(transferData.getSlot())),
                BLSPubkey.fromHexString(transferData.getPubkey()),
                BLSSignature.wrap(Bytes96.fromHexString(transferData.getSignature())));
        transfers.add(transfer);
      }

      if (!blockData.getBody().getVoluntaryExits().isEmpty()) {
        return Optional.of("Implement block voluntary exits!");
      }
      BeaconBlockBody blockBody =
          new BeaconBlockBody(
              BLSSignature.wrap(Bytes96.fromHexString(blockData.getBody().getRandaoReveal())),
              eth1Data1,
              proposerSlashings,
              Collections.emptyList(),
              attestations,
              deposits,
              Collections.emptyList(),
              transfers);
      BeaconBlock block =
          new BeaconBlock(
              SlotNumber.castFrom(UInt64.valueOf(blockData.getSlot())),
              Hash32.fromHexString(blockData.getPreviousBlockRoot()),
              Hash32.fromHexString(blockData.getStateRoot()),
              blockBody,
              BLSSignature.wrap(Bytes96.fromHexString(blockData.getSignature())));

      try {
        BeaconStateEx preBlockState =
            applyEmptySlotTransitionsTillSlot(
                latestState, specHelpers, extendedSlotTransition, block.getSlot());
        postBlockState = perBlockTransition.apply(preBlockState, block);
      } catch (Exception ex) {
        return Optional.of("Error happened during transition: " + ex);
      }
      latestState = postBlockState;
    }

    StateComparator comparator = new StateComparator(testCase.getExpectedState(), latestState);

    return comparator.compare();
  }

  private void initializeStorage(
      SpecHelpers spec, InitialStateTransition initialTransition, BeaconChainStorage chainStorage) {
    BeaconBlock initialGenesis = spec.get_empty_block();
    BeaconStateEx initialState = initialTransition.apply(BeaconStateEx.getEmpty(), initialGenesis);

    Hash32 initialStateRoot = spec.hash_tree_root(initialState);
    BeaconBlock genesis = initialGenesis.withStateRoot(initialStateRoot);
    Hash32 genesisRoot = spec.signed_root(genesis);
    BeaconTuple tuple = BeaconTuple.of(genesis, initialState);

    chainStorage.getTupleStorage().put(tuple);
    chainStorage.getJustifiedStorage().set(genesisRoot);
    chainStorage.getFinalizedStorage().set(genesisRoot);
  }

  private BeaconStateEx applyEmptySlotTransitionsTillSlot(
      BeaconStateEx source,
      SpecHelpers spec,
      ExtendedSlotTransition onSlotTransition,
      SlotNumber slotNumber) {
    BeaconStateEx result = source;
    SlotNumber slotsCnt = slotNumber.minus(source.getSlot());
    for (SlotNumber slot : slotsCnt) {
      result = onSlotTransition.apply(result);
    }

    return result;
  }
}
