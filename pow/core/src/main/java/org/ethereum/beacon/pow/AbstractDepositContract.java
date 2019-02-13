package org.ethereum.beacon.pow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.ssz.Serializer;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class AbstractDepositContract implements DepositContract {

  private final Serializer ssz = Serializer.annotationSerializer();

  private long distanceFromHead;

  private final MonoProcessor<ChainStart> chainStartSink = MonoProcessor.create();
  private final Publisher<ChainStart> chainStartStream = chainStartSink
      .publishOn(Schedulers.single())
      .doOnSubscribe(s -> chainStartSubscribedPriv())
      .name("PowClient.chainStart");

  private final ReplayProcessor<Deposit> depositSink =
      ReplayProcessor.cacheLast();
  private final Publisher<Deposit> depositStream =
      Flux.from(depositSink)
          .publishOn(Schedulers.single())
          .onBackpressureError()
          .name("PowClient.deposit");

  private List<Deposit> initialDeposits = new ArrayList<>();
  private boolean startChainSubscribed;


  protected class DepositEventData {
    public final byte[] deposit_root;
    public final byte[] data;
    public final byte[] merkle_tree_index;
    public final byte[][] merkle_branch;

    public DepositEventData(byte[] deposit_root, byte[] data, byte[] merkle_tree_index,
        byte[][] merkle_branch) {
      this.deposit_root = deposit_root;
      this.data = data;
      this.merkle_tree_index = merkle_tree_index;
      this.merkle_branch = merkle_branch;
    }
  }

  protected synchronized void newDeposit(DepositEventData eventData, byte[] blockHash) {
    if (startChainSubscribed && !chainStartSink.isTerminated()) {
      DepositInfo depositInfo = createDepositInfo(eventData, blockHash);
      initialDeposits.add(depositInfo.getDeposit());
      depositSink.onNext(depositInfo.getDeposit());
    }
  }

  protected synchronized void chainStart(byte[] deposit_root, byte[] time, byte[] blockHash) {
    ChainStart chainStart = new ChainStart(
        Time.castFrom(UInt64.fromBytesBigEndian(Bytes8.wrap(time))),
        new Eth1Data(Hash32.wrap(Bytes32.wrap(deposit_root)),
            Hash32.wrap(Bytes32.wrap(blockHash))),
        initialDeposits);
    chainStartSink.onNext(chainStart);
    chainStartSink.onComplete();
    chainStartDone();
  }

  private void chainStartSubscribedPriv() {
    if (!startChainSubscribed) {
      startChainSubscribed = true;
      chainStartSubscribed();
    }
  }

  protected abstract void chainStartSubscribed();

  protected abstract void chainStartDone();

  @Override
  public Publisher<ChainStart> getChainStartMono() {
    return chainStartStream;
  }

  @Override
  public Publisher<Deposit> getDepositStream() {
    return depositStream;
  }

  private DepositInfo createDepositInfo(DepositEventData eventData, byte[] blockHash) {
    List<Hash32> merkleBranch = Arrays.stream(eventData.merkle_branch)
        .map(bytes -> Hash32.wrap(Bytes32.wrap(bytes)))
        .collect(Collectors.toList());
    Deposit deposit = new Deposit(merkleBranch,
        UInt64.fromBytesBigEndian(Bytes8.wrap(eventData.merkle_tree_index)),
        parseDepositData(eventData.data));
    return new DepositInfo(deposit,
        new Eth1Data(Hash32.wrap(Bytes32.wrap(eventData.deposit_root)),
            Hash32.wrap(Bytes32.wrap(blockHash))));
  }

  private DepositData parseDepositData(byte[] data) {
    Gwei amount = Gwei.castFrom(UInt64.fromBytesBigEndian(Bytes8.wrap(data, 0)));
    Time timestamp = Time.castFrom(UInt64.fromBytesBigEndian(Bytes8.wrap(data, 8)));
    DepositInput depositInput = ssz.decode(BytesValue.wrap(data, 16, data.length - 16),
        DepositInput.class);
    return new DepositData(amount, timestamp, depositInput);
  }

  @Override
  public boolean hasDepositRoot(Hash32 blockHash, Hash32 depositRoot) {
    return hasDepositRootImpl(blockHash.extractArray(), depositRoot.extractArray());
  }

  protected abstract boolean hasDepositRootImpl(byte[] blockHash, byte[] depositRoot);

  @Override
  public Optional<Eth1Data> getLatestEth1Data() {
    return getLatestBlockHashDepositRoot().map(
        r -> new Eth1Data(
            Hash32.wrap(Bytes32.wrap(r.getValue1())),
            Hash32.wrap(Bytes32.wrap(r.getValue0()))));
  }

  protected abstract Optional<Pair<byte[], byte[]>> getLatestBlockHashDepositRoot();

  @Override
  public List<DepositInfo> peekDeposits(int count, Eth1Data fromDepositExclusive,
      Eth1Data tillDepositInclusive) {
    return peekDepositsImpl(count,
        fromDepositExclusive.getBlockHash().extractArray(),
        fromDepositExclusive.getDepositRoot().extractArray(),
        tillDepositInclusive.getBlockHash().extractArray(),
        tillDepositInclusive.getDepositRoot().extractArray())
        .stream()
        .map(blockDepositPair -> createDepositInfo(blockDepositPair.getValue1(), blockDepositPair.getValue0()))
        .collect(Collectors.toList());
  }

  protected abstract List<Pair<byte[], DepositEventData>> peekDepositsImpl(
      int count,
      byte[] startBlockHash, byte[] startDepositRoot,
      byte[] endBlockHash, byte[] endDepositRoot);

  @Override
  public void setDistanceFromHead(long distanceFromHead) {
    this.distanceFromHead = distanceFromHead;
  }

  protected long getDistanceFromHead() {
    return distanceFromHead;
  }
}