package org.ethereum.beacon;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class LocalNetTest {

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
    public List<DepositInfo> peekDeposits(int maxCount, Eth1Data fromDepositExclusive,
        Eth1Data tillDepositInclusive) {
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

  public static void main(String[] args) throws Exception {
    int validatorCount = 4;
    int epochLength = 2;

    ControlledSchedulers schedulers = new ControlledSchedulers();
    Schedulers.set(schedulers);

    Random rnd = new Random(1);
    Time genesisTime = Time.of(10 * 60);
    schedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));
    ChainSpec chainSpec =
        new ChainSpec() {
          @Override
          public SlotNumber.EpochLength getEpochLength() {
            return new SlotNumber.EpochLength(UInt64.valueOf(epochLength));
          }
          @Override
          public Time getSlotDuration() {
            return Time.of(10);
          }

          @Override
          public SlotNumber getGenesisSlot() {
            return SlotNumber.of(1_000_000);
          }

          @Override
          public ValidatorIndex getTargetCommitteeSize() {
            return ValidatorIndex.of(1);
          }
        };

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(chainSpec);

    Pair<List<Deposit>, List<KeyPair>> anyDeposits = TestUtils.getAnyDeposits(specHelpers, validatorCount);
    List<Deposit> deposits = anyDeposits.getValue0();

    LocalWireHub localWireHub = new LocalWireHub(s -> {});
    ChainStart chainStart = new ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    System.out.println("Creating peers...");
    for(int i = 0; i < validatorCount; i++) {
      WireApi wireApi = localWireHub.createNewPeer("" + i);
      Launcher launcher = new Launcher(specHelpers, depositContract, anyDeposits.getValue1().get(i),
          wireApi);
      int finalI = i;
      Flux.from(launcher.slotTicker.getTickerStream())
          .subscribe(slot -> System.out.println("  #" + finalI + " Slot: " + slot.toString(chainSpec, genesisTime)));
      Flux.from(launcher.observableStateProcessor.getObservableStateStream())
          .subscribe(os -> {
            System.out.println("  #" + finalI + " New observable state: " +
                os.getLatestSlotState().getSlot().toString(chainSpec, genesisTime)
                + ": Proposer: " +
                specHelpers.get_beacon_proposer_index(os.getLatestSlotState(), os.getLatestSlotState().getSlot())
                + ", Beacon committee: " +
                specHelpers.get_crosslink_committees_at_slot(os.getLatestSlotState(), os.getLatestSlotState().getSlot()).get(0).getCommittee()
            );
          });
      Flux.from(launcher.beaconChainValidator.getProposedBlocksStream())
          .subscribe(block ->System.out.println("#" + finalI + " !!! New block: " +
              block.toString(chainSpec, genesisTime, specHelpers::hash_tree_root)));
      Flux.from(launcher.beaconChainValidator.getAttestationsStream())
          .subscribe(attest ->System.out.println("#" + finalI + " !!! New attestation: " +
              attest.toString(chainSpec, genesisTime)));
      Flux.from(launcher.beaconChain.getBlockStatesStream())
          .subscribe(blockState ->System.out.println("  #" + finalI + " Block imported: " +
              blockState.getBlock().toString(chainSpec, genesisTime, specHelpers::hash_tree_root)));
    }
    System.out.println("Peers created");

    while (true) {
      schedulers.addTime(Duration.ofSeconds(10));
      System.out.println("===============================");
    }

//    Thread.sleep(100000000);
  }

  static String slotInfo(SpecHelpers specHelpers, Time genesisTime, SlotNumber slot) {
    ChainSpec spec = specHelpers.getChainSpec();
    Time slotTime = genesisTime.plus(spec.getSlotDuration().times(slot.minus(spec.getGenesisSlot())));

    double time = System.currentTimeMillis() - slotTime.getValue() * 1000;
    time /= 1000;
    return "Slot #" + slot.minus(spec.getGenesisSlot()) +
        String.format(" %.1f sec from its time", time);
  }
}
