package org.ethereum.beacon.pow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.SSZSerializerBuilder;
import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class AbstractDepositContract implements DepositContract {
  private final SSZSerializer ssz = SSZSerializerBuilder.getBakedAnnotationBuilder().build();

  private final MonoProcessor<ChainStart> chainStartSink = MonoProcessor.create();
  private final Publisher<ChainStart> chainStartStream = chainStartSink
      .publishOn(Schedulers.single())
      .doOnSubscribe(s -> chainStartSubscribed())
      .name("PowClient.chainStart");
  private final ReplayProcessor<DepositInfo> depositSink = ReplayProcessor.cacheLast();
  private final Publisher<DepositInfo> depositStream = depositSink
      .publishOn(Schedulers.single())
      .doOnSubscribe(s -> depositSubscriptionsChanged())
      .doOnCancel(() -> depositSubscriptionsChanged())
      .onBackpressureError()
      .name("PowClient.deposits");

  private boolean depositsSubscribed;
  private List<Deposit> initialDeposits = new ArrayList<>();


  protected synchronized void newDeposit(byte[] deposit_root,
      byte[] data, byte[] merkle_tree_index, byte[][] merkle_branch, byte[] blockHash) {

    List<Hash32> merkleBranch = Arrays.stream(merkle_branch)
        .map(bytes -> Hash32.wrap(Bytes32.wrap(bytes)))
        .collect(Collectors.toList());
    Deposit deposit = new Deposit(merkleBranch,
            UInt64.fromBytesBigEndian(Bytes8.wrap(merkle_tree_index)), parseDepositData(data));
    DepositInfo depositInfo = new DepositInfo(deposit,
        new Eth1Data(Hash32.wrap(Bytes32.wrap(deposit_root)),
            Hash32.wrap(Bytes32.wrap(blockHash))));

    if (chainStartSink.isSuccess()) {
      depositSink.onNext(depositInfo);
    } else {
      initialDeposits.add(deposit);
    }
  }

  private DepositData parseDepositData(byte[] data) {
    UInt64 amount = UInt64.fromBytesBigEndian(Bytes8.wrap(data, 0));
    UInt64 timestamp = UInt64.fromBytesBigEndian(Bytes8.wrap(data, 8));
    DepositInput depositInput = ssz.decode(Arrays.copyOfRange(data, 16, data.length),
        DepositInput.class);
    return new DepositData(depositInput, amount, timestamp);
  }

  protected synchronized void chainStart(byte[] deposit_root, byte[] time, byte[] blockHash) {
    ChainStart chainStart = new ChainStart(
        UInt64.fromBytesBigEndian(Bytes8.wrap(time)),
        new Eth1Data(Hash32.wrap(Bytes32.wrap(deposit_root)),
            Hash32.wrap(Bytes32.wrap(blockHash))),
        initialDeposits);
    chainStartSink.onNext(chainStart);
    chainStartSink.onComplete();
  }

  private synchronized void depositSubscriptionsChanged() {
    if (!depositsSubscribed && depositSink.hasDownstreams()) {
      depositsSubscribed();
    }
    if (depositsSubscribed && !depositSink.hasDownstreams()) {
      depositsUnsubscribed();
    }

    depositsSubscribed = depositSink.hasDownstreams();
  }

  protected abstract void chainStartSubscribed();

  protected abstract void depositsSubscribed();

  protected abstract void depositsUnsubscribed();

  @Override
  public Publisher<ChainStart> getChainStartMono() {
    return chainStartStream;
  }

  @Override
  public Publisher<DepositInfo> getAfterDepositsStream() {
    return depositStream;
  }
}
