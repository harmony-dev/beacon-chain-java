package org.ethereum.beacon;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.LoggerMDCExecutor;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.wire.LocalWireHub;
import org.ethereum.beacon.wire.WireApi;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
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

  static class MDCControlledSchedulers {
    public String mdcKey = "validatorIndex";
    private int counter = 0;
    private List<ControlledSchedulers> schedulersList = new ArrayList<>();
    private long currentTime;

    public ControlledSchedulers createNew() {
      LoggerMDCExecutor mdcExecutor = new LoggerMDCExecutor(mdcKey, "" + counter);
      counter++;
      ControlledSchedulers newSched = Schedulers.createControlled(() -> mdcExecutor);
      newSched.setCurrentTime(currentTime);
      schedulersList.add(newSched);
      return newSched;
    }

    public void setCurrentTime(long time) {
      currentTime = time;
      schedulersList.forEach(cs -> cs.setCurrentTime(time));
    }

    void addTime(Duration duration) {
      addTime(duration.toMillis());
    }

    void addTime(long millis) {
      setCurrentTime(currentTime + millis);
    }
  }

  public static void main(String[] args) throws Exception {
    int validatorCount = 8;
    int epochLength = 4;
    int targetCommitteeSize = 2;
    int shardCount = 4;
    Random rnd = new Random(1);
    Time genesisTime = Time.of(10 * 60);

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));

    SpecConstants specConstants =
        new SpecConstants() {
          @Override
          public SlotNumber.EpochLength getSlotsPerEpoch() {
            return new SlotNumber.EpochLength(UInt64.valueOf(epochLength));
          }
          @Override
          public Time getSecondsPerSlot() {
            return Time.of(10);
          }

          @Override
          public SlotNumber getGenesisSlot() {
            return SlotNumber.of(1_000_000);
          }

          @Override
          public ValidatorIndex getTargetCommitteeSize() {
            return ValidatorIndex.of(targetCommitteeSize);
          }

          @Override
          public SlotNumber getMinAttestationInclusionDelay() {
            return SlotNumber.of(1);
          }

          @Override
          public ShardNumber getShardCount() {
            return ShardNumber.of(shardCount);
          }
        };

    Pair<List<Deposit>, List<KeyPair>> anyDeposits = TestUtils.getAnyDeposits(
            rnd, createLightSpecHelpers(specConstants, () -> 0L), validatorCount);
    List<Deposit> deposits = anyDeposits.getValue0();

    LocalWireHub localWireHub = new LocalWireHub(s -> {});
    ChainStart chainStart = new ChainStart(genesisTime, eth1Data, deposits);
    DepositContract depositContract = new SimpleDepositContract(chainStart);

    System.out.println("Creating peers...");
    List<Launcher> peers = new ArrayList<>();
    for(int i = 0; i < validatorCount; i++) {
      ControlledSchedulers schedulers = controlledSchedulers.createNew();
      SpecHelpers specHelpers = createLightSpecHelpers(specConstants, schedulers::getCurrentTime);
      WireApi wireApi = localWireHub.createNewPeer("" + i);

      Launcher launcher = new Launcher(specHelpers, depositContract, anyDeposits.getValue1().get(i),
          wireApi, new MemBeaconChainStorageFactory(), schedulers);
      peers.add(launcher);

      int finalI = i;
      Flux.from(launcher.slotTicker.getTickerStream())
          .subscribe(slot -> System.out.println("  #" + finalI + " Slot: " + slot.toString(
              specConstants, genesisTime)));
      Flux.from(launcher.observableStateProcessor.getObservableStateStream())
          .subscribe(os -> {
            System.out.println("  #" + finalI + " New observable state: " + os.toString(specHelpers));
          });
      Flux.from(launcher.beaconChainValidator.getProposedBlocksStream())
          .subscribe(block ->System.out.println("#" + finalI + " !!! New block: " +
              block.toString(specConstants, genesisTime, specHelpers::hash_tree_root)));
      Flux.from(launcher.beaconChainValidator.getAttestationsStream())
          .subscribe(attest ->System.out.println("#" + finalI + " !!! New attestation: " +
              attest.toString(specConstants, genesisTime)));
      Flux.from(launcher.beaconChain.getBlockStatesStream())
          .subscribe(blockState ->System.out.println("  #" + finalI + " Block imported: " +
              blockState.getBlock().toString(specConstants, genesisTime, specHelpers::hash_tree_root)));
    }
    System.out.println("Peers created");

    AtomicReference<SlotNumber> curSlot = new AtomicReference<>();
    Set<Attestation> attestations = new HashSet<>();
    Set<Attestation> blockAttestations = new HashSet<>();
    Set<Attestation> allBlockAttestations = new HashSet<>();
    Set<Attestation> stateAttestations = new HashSet<>();

    WireApi wireApi = localWireHub.createNewPeer("test");
    Flux.from(wireApi.inboundAttestationsStream())
        .subscribe(att -> {
          if (!attestations.add(segregate(att).get(0))) {
            System.err.println("Duplicate attestation: " + att);
          }
        });
    Flux.from(wireApi.inboundBlocksStream())
        .subscribe(block -> block.getBody().getAttestations().stream()
            .flatMap(aggAtt -> segregate(aggAtt).stream())
            .forEach(att -> {
                if (!attestations.remove(att)) {
                  if (allBlockAttestations.contains(att)) {
                    System.err.println("Double included attestation: " + att);
                  } else {
                    System.err.println("Unknown attestation: " + att);
                  }
                }
                if (allBlockAttestations.contains(att)) {
                  System.err.println("Duplicate block attestation: " + att);
                } else {
                  blockAttestations.add(att);
                  allBlockAttestations.add(att);
                }
        }));
    Flux.from(peers.get(0).observableStateProcessor.getObservableStateStream())
        .subscribe(state -> {
          state.getLatestSlotState().getLatestAttestations().stream()
              .flatMap(a -> fromPending(a).stream())
              .forEach(att -> {
                blockAttestations.remove(att);
                  if (!allBlockAttestations.contains(att)) {
                    System.err.println("Unknown block attestation: " + att);
                  }
                  stateAttestations.add(att);
              });
          curSlot.set(state.getLatestSlotState().getSlot());
        });

    Map<SlotNumber, ObservableBeaconState> slotStates = new HashMap<>();
    SpecHelpers specHelpers = peers.get(0).spec;
    Flux.from(peers.get(0).observableStateProcessor.getObservableStateStream())
        .subscribe((ObservableBeaconState state) -> {

          SlotNumber slot = state.getLatestSlotState().getSlot();
          slotStates.put(slot, state);
          if (slotStates.size() > epochLength) {
            SlotNumber oldSlot = slot.minus(epochLength);
            List<ShardCommittee> committees1 = specHelpers
                .get_crosslink_committees_at_slot(slotStates.get(oldSlot).getLatestSlotState(), oldSlot);
            List<ShardCommittee> committees2 = specHelpers
                .get_crosslink_committees_at_slot(state.getLatestSlotState(), oldSlot);
            if (!committees1.equals(committees2)) {
              System.err.println("##### Committees differ!!!");
            }
          }
        });


    while (true) {
      System.out.println("===============================");
      controlledSchedulers.addTime(Duration.ofSeconds(10));

      attestations.stream()
          .filter(att -> att.getData().getSlot().less(curSlot.get().minus(epochLength * 2)))
          .forEach(att -> System.err.println("###### Lost attestation: " + att));

      blockAttestations.stream()
          .filter(att -> att.getData().getSlot().less(curSlot.get().minus(epochLength * 2)))
          .forEach(att -> System.err.println("###### Lost block attestation: " + att));
    }
  }

  static List<Attestation> segregate(Attestation attestation) {
    List<Attestation> ret = new ArrayList<>();
    for (Integer validatorBit : attestation.getAggregationBitfield().getBits()) {
      Bitfield singleBit = Bitfield.of(MutableBytesValue.create(attestation.getAggregationBitfield().size()))
          .setBit(validatorBit, true);
      ret.add(new Attestation(
          attestation.getData(),
          singleBit,
          attestation.getCustodyBitfield(),
          BLSSignature.ZERO));
    }
    return ret;
  }

  static List<Attestation> fromPending(PendingAttestationRecord attestation) {
    return segregate(
        new Attestation(
            attestation.getData(),
            attestation.getAggregationBitfield(),
            attestation.getCustodyBitfield(),
            BLSSignature.ZERO));
  }

  static SpecHelpers createLightSpecHelpers(SpecConstants spec, Supplier<Long> time) {
    return new SpecHelpers(spec, Hashes::keccak256, SSZObjectHasher.create(Hashes::keccak256), time) {
      @Override
      public boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature, Bytes8 domain) {
        return true;
      }

      @Override
      public boolean bls_verify_multiple(List<PublicKey> publicKeys, List<Hash32> messages,
          BLSSignature signature, Bytes8 domain) {
        return true;
      }
    };
  }
}
