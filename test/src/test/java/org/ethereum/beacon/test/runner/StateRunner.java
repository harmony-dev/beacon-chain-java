package org.ethereum.beacon.test.runner;

import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.simulator.SimulatorLauncher;
import org.ethereum.beacon.test.type.StateTestCase;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.util.Objects;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** TestRunner for {@link StateTestCase} */
public class StateRunner implements Runner {
  private StateTestCase testCase;
  private Function<SpecConstants, SpecHelpers> specHelpersBuilder;

  public StateRunner(TestCase testCase, Function<SpecConstants, SpecHelpers> specHelpersBuilder) {
    if (!(testCase instanceof StateTestCase)) {
      throw new RuntimeException("TestCase runner accepts only StateTestCase.class as input!");
    }
    this.testCase = (StateTestCase) testCase;
    this.specHelpersBuilder = specHelpersBuilder;
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

    InMemoryDatabase db = new InMemoryDatabase();
    BeaconChainStorageFactory storageFactory = new MemBeaconChainStorageFactory();
    BeaconChainStorage beaconChainStorage = storageFactory.create(db);

    BeaconBlockVerifier blockVerifier = BeaconBlockVerifier.createDefault(specHelpers);
    BeaconStateVerifier stateVerifier = BeaconStateVerifier.createDefault(specHelpers);

    ControlledSchedulers schedulers =
        new SimulatorLauncher.MDCControlledSchedulers().createNew("Main");
    MutableBeaconChain beaconChain =
        new DefaultBeaconChain(
            specHelpers,
            initialTransition,
            perSlotTransition,
            perBlockTransition,
            perEpochTransition,
            blockVerifier,
            stateVerifier,
            beaconChainStorage,
            schedulers);
    beaconChain.init();

    List<ValidatorRecord> validators = new ArrayList<>();
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
    }

    return Optional.of("Implement me!");
  }
}
