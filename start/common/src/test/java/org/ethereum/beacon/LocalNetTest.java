package org.ethereum.beacon;

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
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
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
    int validatorCount = 8;

    Random rnd = new Random();
    Time genesisTime = Time.castFrom(UInt64.valueOf(System.currentTimeMillis() / 1000 - 60));
    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));
    ChainSpec chainSpec =
        new ChainSpec() {
          @Override
          public SlotNumber.EpochLength getEpochLength() {
            return new SlotNumber.EpochLength(UInt64.valueOf(validatorCount));
          }
          @Override
          public Time getSlotDuration() {
            return Time.of(5);
          }
        };

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(chainSpec);

    Pair<List<Deposit>, List<KeyPair>> anyDeposits = TestUtils.getAnyDeposits(specHelpers, validatorCount);
    List<Deposit> deposits = anyDeposits.getValue0();

    LocalWireHub localWireHub = new LocalWireHub(s -> System.out.println(new Date() + " : " + s));
    ChainStart chainStart = new ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    System.out.println("Creating peers...");
    for(int i = 0; i < validatorCount; i++) {
      WireApi wireApi = localWireHub.createNewPeer("" + i);
      Launcher launcher = new Launcher(specHelpers, depositContract, anyDeposits.getValue1().get(i),
          wireApi);
      Flux.from(launcher.slotTicker.getTickerStream())
          .subscribe(slot -> System.out.println(slotInfo(specHelpers, genesisTime, slot)));
      Flux.from(launcher.observableStateProcessor.getObservableStateStream())
          .subscribe(os ->System.out.println("New observable state: " +
              slotInfo(specHelpers, genesisTime, os.getLatestSlotState().getSlot())));
    }
    System.out.println("Peers created");

    Thread.sleep(100000000);
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
