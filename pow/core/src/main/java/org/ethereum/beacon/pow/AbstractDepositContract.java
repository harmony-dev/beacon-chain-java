package org.ethereum.beacon.pow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.javatuples.Pair;
import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoProcessor;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class AbstractDepositContract implements DepositContract {
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

  private final SSZSerializer ssz = new SSZBuilder().buildSerializer();

  private long distanceFromHead;

  protected final Schedulers schedulers;
  private final MonoProcessor<ChainStart> chainStartSink = MonoProcessor.create();
  private final Publisher<ChainStart> chainStartStream;

  private final SimpleProcessor<Deposit> depositStream;

  private List<Deposit> initialDeposits = new ArrayList<>();
  private boolean startChainSubscribed;

  public AbstractDepositContract(Schedulers schedulers) {
    this.schedulers = schedulers;

    chainStartStream = chainStartSink
        .publishOn(this.schedulers.reactorEvents())
        .doOnSubscribe(s -> chainStartSubscribedPriv())
        .name("PowClient.chainStart");
    depositStream = new SimpleProcessor<>(this.schedulers.reactorEvents(), "PowClient.deposit");
  }

  protected synchronized void newDeposit(DepositEventData eventData, byte[] blockHash) {
    if (startChainSubscribed && !chainStartSink.isTerminated()) {
      DepositInfo depositInfo = createDepositInfo(eventData, blockHash);
      initialDeposits.add(depositInfo.getDeposit());
      depositStream.onNext(depositInfo.getDeposit());
    }
  }

  protected synchronized void chainStart(byte[] deposit_root, byte[] time, byte[] blockHash) {
    ChainStart chainStart = new ChainStart(
        Time.castFrom(UInt64.fromBytesBigEndian(Bytes8.wrap(time))),
        new Eth1Data(Hash32.wrap(Bytes32.wrap(deposit_root)),
            UInt64.valueOf(initialDeposits.size()),
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
    Deposit deposit = new Deposit(ReadVector.wrap(merkleBranch, Function.identity()),
        UInt64.fromBytesBigEndian(Bytes8.wrap(eventData.merkle_tree_index)),
        parseDepositData(eventData.data));
    return new DepositInfo(deposit,
        new Eth1Data(Hash32.wrap(Bytes32.wrap(eventData.deposit_root)),
            UInt64.ZERO,
            Hash32.wrap(Bytes32.wrap(blockHash))));
  }

  private DepositData parseDepositData(byte[] data) {
    BLSPubkey pubkey = BLSPubkey.wrap(Bytes48.wrap(data, 0));
    Hash32 withdrawalCredentials = Hash32.wrap(Bytes32.wrap(data, 48));
    Gwei amount =
        Gwei.castFrom(UInt64.fromBytesLittleEndian(Bytes8.wrap(data, Bytes48.SIZE + Bytes32.SIZE)));
    BLSSignature signature =
        BLSSignature.wrap(Bytes96.wrap(data, Bytes48.SIZE + Bytes32.SIZE + Bytes8.SIZE));
    return new DepositData(pubkey, withdrawalCredentials, amount, signature);
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
            UInt64.ZERO,
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