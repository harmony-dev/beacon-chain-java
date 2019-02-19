package org.ethereum.beacon;

import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.emulator.config.chainspec.ChainSpecData;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.action.Action;
import org.ethereum.beacon.emulator.config.main.action.ActionEmulate;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.util.EmulateUtils;
import org.ethereum.beacon.wire.LocalWireHub;
import org.ethereum.beacon.wire.WireApi;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import picocli.CommandLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;

@CommandLine.Command(
    description = "Eth2.0 emulator",
    name = "beacon-emulator",
    mixinStandardHelpOptions = true,
    version = "beacon-emulator " + ReusableOptions.VERSION)
public class Emulator extends ReusableOptions implements Callable<Void> {
  @CommandLine.Parameters(
      index = "0",
      description =
          "Task to do: start/print.\n start - Starts beacon emulator.\n print - Prints configuration and tasks to run on start.")
  Task action;

  @CommandLine.Parameters(
      index = "1",
      description = "Number of validators to emulate.",
      arity = "0..1")
  Integer validators;

  public static void main(String[] args) {
    CommandLine.call(new Emulator(), args);
  }

  @Override
  public Void call() throws Exception {
    System.out.println("Starting beacon emulator...");
    if (validators != null) {
      configPathValues.add(Pair.with("plan.validator[0].count", validators));
    }
    Pair<MainConfig, ChainSpecData> configs =
        prepareAndPrintConfigs(action, "/config/emulator-config.yml", "/config/emulator-chainSpec.yml");

    if (action.equals(Task.start)) {
      runEmulator(configs.getValue0(), configs.getValue1().build());
    }

    return null;
  }

  private void runEmulator(MainConfig mainConfig, ChainSpec chainSpec) {
    List<Action> actions = mainConfig.getPlan().getValidator();
    Optional<ActionEmulate> actionEmulate =
        actions.stream()
            .filter(a -> a instanceof ActionEmulate)
            .map(a -> (ActionEmulate) a)
            .findFirst();
    if (!actionEmulate.isPresent()) {
      throw new RuntimeException("Emulate settings are not set");
    }
    int validatorCount = actionEmulate.get().getCount();

    ControlledSchedulers schedulers = new ControlledSchedulers();
    Schedulers.set(schedulers);

    Random rnd = new Random(1);
    Time genesisTime = Time.of(10 * 60);
    schedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));
    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(chainSpec);

    Pair<List<Deposit>, List<BLS381.KeyPair>> anyDeposits =
        EmulateUtils.getAnyDeposits(specHelpers, validatorCount);
    List<Deposit> deposits = anyDeposits.getValue0();

    LocalWireHub localWireHub = new LocalWireHub(s -> {});
    DepositContract.ChainStart chainStart =
        new DepositContract.ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    System.out.println("Creating peers...");
    for (int i = 0; i < validatorCount; i++) {
      WireApi wireApi = localWireHub.createNewPeer("" + i);
      Launcher launcher =
          new Launcher(
              specHelpers,
              depositContract,
              anyDeposits.getValue1().get(i),
              wireApi,
              new MemBeaconChainStorageFactory());

      int finalI = i;
      Flux.from(launcher.slotTicker.getTickerStream())
          .subscribe(
              slot ->
                  System.out.println(
                      "  #" + finalI + " Slot: " + slot.toString(chainSpec, genesisTime)));
      Flux.from(launcher.observableStateProcessor.getObservableStateStream())
          .subscribe(
              os -> {
                System.out.println(
                    "  #" + finalI + " New observable state: " + os.toString(specHelpers));
              });
      Flux.from(launcher.beaconChainValidator.getProposedBlocksStream())
          .subscribe(
              block ->
                  System.out.println(
                      "#"
                          + finalI
                          + " !!! New block: "
                          + block.toString(chainSpec, genesisTime, specHelpers::hash_tree_root)));
      Flux.from(launcher.beaconChainValidator.getAttestationsStream())
          .subscribe(
              attest ->
                  System.out.println(
                      "#"
                          + finalI
                          + " !!! New attestation: "
                          + attest.toString(chainSpec, genesisTime)));
      Flux.from(launcher.beaconChain.getBlockStatesStream())
          .subscribe(
              blockState ->
                  System.out.println(
                      "  #"
                          + finalI
                          + " Block imported: "
                          + blockState
                              .getBlock()
                              .toString(chainSpec, genesisTime, specHelpers::hash_tree_root)));
    }
    System.out.println("Peers created");

    while (true) {
      System.out.println("===============================");
      schedulers.addTime(Duration.ofSeconds(10));
    }
  }

  private static class SimpleDepositContract implements DepositContract {
    private final ChainStart chainStart;

    public SimpleDepositContract(ChainStart chainStart) {
      this.chainStart = chainStart;
    }

    @Override
    public Publisher<ChainStart> getChainStartMono() {
      return Mono.just(chainStart);
    }

    @Override
    public Publisher<Deposit> getDepositStream() {
      return Mono.empty();
    }

    @Override
    public List<DepositInfo> peekDeposits(
        int maxCount, Eth1Data fromDepositExclusive, Eth1Data tillDepositInclusive) {
      return Collections.emptyList();
    }

    @Override
    public boolean hasDepositRoot(Hash32 blockHash, Hash32 depositRoot) {
      return true;
    }

    @Override
    public Optional<Eth1Data> getLatestEth1Data() {
      return Optional.of(chainStart.getEth1Data());
    }

    @Override
    public void setDistanceFromHead(long distanceFromHead) {}
  }
}
