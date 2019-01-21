package org.ethereum.beacon.validator;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.ethereum.beacon.chain.BeaconChainListener;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.pending.ObservableChainState;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.PoWChainListener;
import org.ethereum.beacon.randao.Randao;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

/** Runs a single validator in the same instance with chain processing. */
public class SingleValidatorService implements ValidatorService {

  private static final String RANDAO_SOURCE = "randao_source";
  private static final int RANDAO_ROUNDS = 1 << 20;

  private BeaconChainListener beaconChainListener;
  private PoWChainListener powChainListener;
  private DepositContract depositContract;
  private ValidatorCredentials credentials;
  private Database database;
  private ValidatorRegistrar registrar;

  private ChainSpec chainSpec;
  private SpecHelpers specHelpers;

  private Randao randao;
  private ObservableChainState head;
  private Hash32 depositRoot;
  private Optional<UInt24> index = Optional.empty();

  private ScheduledExecutorService taskExecutor;

  public SingleValidatorService(
      BeaconChainListener beaconChainListener,
      PoWChainListener powChainListener,
      DepositContract depositContract,
      ValidatorCredentials credentials,
      Database database,
      ValidatorRegistrar registrar,
      ChainSpec chainSpec,
      SpecHelpers specHelpers) {
    this.beaconChainListener = beaconChainListener;
    this.powChainListener = powChainListener;
    this.depositContract = depositContract;
    this.credentials = credentials;
    this.database = database;
    this.registrar = registrar;
    this.chainSpec = chainSpec;
    this.specHelpers = specHelpers;
  }

  @Override
  public void start() {
    DataSource<BytesValue, BytesValue> randaoSource = database.createStorage(RANDAO_SOURCE);
    if (!registrar.isRegistered(credentials)) {
      this.randao = Randao.create(randaoSource, RANDAO_ROUNDS);
      if (!registrar.register(credentials, this.randao)) {
        throw new RuntimeException("Unable to register validator " + credentials);
      }
    } else {
      this.randao = Randao.get(randaoSource);
    }

    this.taskExecutor =
        Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
              @Override
              public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "validator-service");
                t.setDaemon(true);
                return t;
              }
            });

    waitForBeaconChainSync();
    waitForPoWChainSync();
    this.depositRoot = depositContract.getRecentDepositRoot();

    waitForTheFirstHead(this::processFirstHead);
    subscribeToHeadUpdates(this::processNewHead);
    subscribeToDepositRootUpdates(depositRoot -> this.depositRoot = depositRoot);
  }

  @Override
  public void stop() {
    this.taskExecutor.shutdown();
  }

  private void processFirstHead(ObservableChainState head) {
    UInt24 index =
        specHelpers.get_validator_index_by_pubkey(
            head.getRecentSlotState(), credentials.getBlsPublicKey());
    Preconditions.checkState(
        index.compareTo(UInt24.MAX_VALUE) < 0,
        "validator with given pubKey is not found, pubKey %s",
        credentials.getBlsPublicKey());

    this.index = Optional.of(index);
    createTasks(head.getRecentSlotState(), index);
  }

  private void processNewHead(ObservableChainState head) {
    this.head = head;
    if (specHelpers.is_epoch_transition(head.getRecentSlotState().getSlot()) && index.isPresent()) {
      index.ifPresent(index -> createTasks(head.getRecentSlotState(), index));
    }
  }

  private void createTasks(BeaconState state, UInt24 index) {
    createNextProposerTask(state, index);
    createNextAttesterTask(state, index);
  }

  // TODO slot calculation
  private void createNextProposerTask(BeaconState state, UInt24 index) {
    UInt64 slot = UInt64.ZERO;
    if (slot.compareTo(UInt64.MAX_VALUE) < 0) {
      scheduleOnStart(slot, this::proposerRoutine);
    }
  }

  // TODO slot calculation
  private void createNextAttesterTask(BeaconState state, UInt24 index) {
    UInt64 slot = UInt64.ZERO;
    if (slot.compareTo(UInt64.MAX_VALUE) < 0) {
      scheduleInTheMiddle(slot, this::attesterRoutine);
    }
  }

  private void scheduleOnStart(UInt64 slot, Runnable routine) {
    long startsAt = slot.times(chainSpec.getSlotDuration()).times(1000).getValue();
    taskExecutor.schedule(routine, startsAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  private void scheduleInTheMiddle(UInt64 slot, Runnable routine) {
    long startsAt =
        slot.times(chainSpec.getSlotDuration())
            .plus(chainSpec.getSlotDuration().dividedBy(2))
            .times(1000)
            .getValue();
    taskExecutor.schedule(routine, startsAt - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
  }

  private void proposerRoutine() {}

  private void attesterRoutine() {}

  /** FIXME: a stub for subscription. */
  private void subscribeToHeadUpdates(Consumer<ObservableChainState> payload) {}

  /** FIXME: a stub for subscription. */
  private void subscribeToDepositRootUpdates(Consumer<Hash32> payload) {}

  /** FIXME: a stub for subscription. */
  private void waitForTheFirstHead(Consumer<ObservableChainState> payload) {}

  private void waitForBeaconChainSync() {
    try {
      beaconChainListener.getSyncDone().get();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private void waitForPoWChainSync() {
    try {
      powChainListener.getSyncDone().get();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
