package org.ethereum.beacon.time.mapper;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.junit.Test;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.LongStream;

public class ObjectTimeMapperTest {

  @Test
  public void testBlock() {
    BeaconChainSpec spec =
        BeaconChainSpec.createWithDefaultHasher(
            new SpecConstants() {
              @Override
              public SlotNumber getGenesisSlot() {
                return SlotNumber.ZERO;
              }

              @Override
              public Time getSecondsPerSlot() {
                return Time.of(1);
              }
            });
    MutableBeaconState state = BeaconState.getEmpty().createMutableCopy();
    long startTime = 1000;
    state.setGenesisTime(Time.of(startTime));
    SimpleProcessor<BeaconBlock> blockStream =
        new SimpleProcessor<>(Schedulers.createDefault().events(), "test");

    ObjectTimeMapper<BeaconBlock> blockMapper =
        new ObjectTimeMapper<>(
            Schedulers.createDefault().events(),
            blockStream,
            block -> spec.get_slot_start_time(state, block.getSlot()));

    CountDownLatch getFive = new CountDownLatch(5);
    Flux.from(blockMapper.getTimeStream())
        .subscribe(
            new Consumer<Time>() {
              private int counter = 0;

              @Override
              public void accept(Time time) {
                if (time.getValue() == (startTime + counter)) {
                  getFive.countDown();
                  ++counter;
                }
              }
            });

    LongStream.range(0, 5).mapToObj(this::createBlockWithSlot).forEach(blockStream::onNext);
    try {
      assert getFive.await(100, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private BeaconBlock createBlockWithSlot(long slot) {
    return BeaconBlock.Builder.createEmpty()
        .withBody(BeaconBlockBody.EMPTY)
        .withParentRoot(Hash32.ZERO)
        .withSignature(BLSSignature.ZERO)
        .withStateRoot(Hash32.ZERO)
        .withSlot(SlotNumber.of(slot))
        .build();
  }
}
