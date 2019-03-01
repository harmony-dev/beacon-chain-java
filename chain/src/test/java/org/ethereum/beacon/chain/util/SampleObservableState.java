package org.ethereum.beacon.chain.util;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.SlotTicker;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.ObservableStateProcessorImpl;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class SampleObservableState {
  private final SpecHelpers specHelpers;

  public List<Deposit> deposits;
  public List<KeyPair> depositKeys;
  public Eth1Data eth1Data;
  public ChainStart chainStart;
  public InMemoryDatabase db;
  public BeaconChainStorage beaconChainStorage;
  public MutableBeaconChain beaconChain;
  public SlotTicker slotTicker;
  public ObservableStateProcessor observableStateProcessor;

  public SampleObservableState(
      Random rnd,
      Duration genesisTime,
      long genesisSlot,
      Duration slotDuration,
      int validatorCount,
      Publisher<Attestation> attestationsSteam,
      Schedulers schedulers) {

    ChainSpec chainSpec =
        new ChainSpec() {
          @Override
          public SlotNumber.EpochLength getSlotsPerEpoch() {
            return new SlotNumber.EpochLength(UInt64.valueOf(validatorCount));
          }

          @Override
          public Time getSecondsPerSlot() {
            return Time.of(slotDuration.getSeconds());
          }

          @Override
          public SlotNumber getGenesisSlot() {
            return SlotNumber.of(genesisSlot);
          }
        };
    this.specHelpers = SpecHelpers.createWithSSZHasher(chainSpec, schedulers::getCurrentTime);

    Pair<List<Deposit>, List<KeyPair>> anyDeposits = TestUtils
        .getAnyDeposits(rnd, specHelpers, 8);
    deposits = anyDeposits.getValue0();
    depositKeys = anyDeposits.getValue1();

    eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));
    chainStart = new ChainStart(Time.of(genesisTime.getSeconds()), eth1Data, deposits);

    InitialStateTransition initialTransition = new InitialStateTransition(chainStart, specHelpers);
    PerSlotTransition perSlotTransition = new PerSlotTransition(specHelpers);
    PerBlockTransition perBlockTransition = new PerBlockTransition(specHelpers);
    PerEpochTransition perEpochTransition = new PerEpochTransition(specHelpers);

    db = new InMemoryDatabase();
    beaconChainStorage = BeaconChainStorageFactory.get().create(db);

    // TODO
    BeaconBlockVerifier blockVerifier = (block, state) -> VerificationResult.PASSED;
    // TODO
    BeaconStateVerifier stateVerifier = (block, state) -> VerificationResult.PASSED;

    beaconChain = new DefaultBeaconChain(
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

    slotTicker = new SlotTicker(specHelpers, beaconChain.getRecentlyProcessed().getState(),
        schedulers);
    slotTicker.start();

    observableStateProcessor = new ObservableStateProcessorImpl(
        beaconChainStorage,
        slotTicker.getTickerStream(),
        attestationsSteam,
        beaconChain.getBlockStatesStream(),
        specHelpers,
        perSlotTransition,
        perEpochTransition,
        schedulers);
    observableStateProcessor.start();

  }
}
