package org.ethereum.beacon.pow;

import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoProcessor;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractDepositContract implements DepositContract {
  protected final Schedulers schedulers;
  private final MonoProcessor<ChainStart> chainStartSink = MonoProcessor.create();
  private final Publisher<ChainStart> chainStartStream;
  private final SimpleProcessor<Deposit> depositStream;
  private final MerkleTree<DepositData> tree;
  private long distanceFromHead;
  private List<Deposit> initialDeposits = new ArrayList<>();
  private boolean startChainSubscribed;

  public AbstractDepositContract(
      Schedulers schedulers, Function<BytesValue, Hash32> hashFunction, int treeDepth) {
    this.schedulers = schedulers;

    chainStartStream =
        chainStartSink
            .publishOn(this.schedulers.reactorEvents())
            .doOnSubscribe(s -> chainStartSubscribedPriv())
            .name("PowClient.chainStart");
    depositStream = new SimpleProcessor<>(this.schedulers.reactorEvents(), "PowClient.deposit");
    this.tree = new DepositBufferedMerkle(hashFunction, treeDepth, 1000);
  }

  /**
   * Stores deposits data from invocation list eventDataList
   *
   * @param eventDataList All deposit events in blockHash
   * @param blockHash Block hash
   */
  protected synchronized void newDeposits(List<DepositEventData> eventDataList, byte[] blockHash) {
    List<Deposit> deposits =
        eventDataList.stream().map(this::newDeposit).collect(Collectors.toList());
    if (deposits.isEmpty()) {
      return;
    }
    int size = deposits.get(deposits.size() - 1).getIndex().increment().intValue();
    for (Deposit deposit : deposits) {
      Deposit depositProofed =
          new Deposit(
              ReadVector.wrap(tree.getProof(deposit.getIndex().intValue(), size), Integer::new),
              deposit.getIndex(),
              deposit.getData());
      if (startChainSubscribed && !chainStartSink.isTerminated()) {
        initialDeposits.add(depositProofed);
      }
      depositStream.onNext(depositProofed);
    }
  }

  /**
   * Same as {@link #newDeposits(List, byte[])} but doesn't store deposits data, instead expects its
   * already stored
   */
  private List<DepositInfo> restoreDeposits(List<DepositEventData> eventData, byte[] blockHash) {
    List<Deposit> deposits =
        eventData.stream().map(this::createUnProofedDeposit).collect(Collectors.toList());
    if (deposits.isEmpty()) {
      return Collections.emptyList();
    }
    int size = deposits.get(0).getIndex().plus(deposits.size()).intValue();
    return deposits.stream()
        .map(
            deposit ->
                new Deposit(
                    ReadVector.wrap(tree.getProof(deposit.getIndex().intValue(), size), Integer::new),
                    deposit.getIndex(),
                    deposit.getData()))
        .map(
            d ->
                new DepositInfo(
                    d,
                    new Eth1Data(
                        tree.getRoot(d.getIndex().intValue()),
                        d.getIndex().decrement(),
                        Hash32.wrap(Bytes32.wrap(blockHash)))))
        .collect(Collectors.toList());
  }

  /**
   * Inserts deposit in storage and returns it NOTE: returns Deposit with empty proof, proof should
   * be filled by someone else
   *
   * @param eventData Deposit event
   * @return Deposit
   */
  private Deposit newDeposit(DepositEventData eventData) {
    Deposit deposit = createUnProofedDeposit(eventData);
    tree.addValue(deposit.getData());
    return deposit;
  }

  public Hash32 getDepositRoot(byte[] merkleTreeIndex) {
    UInt64 index = UInt64.fromBytesLittleEndian(Bytes8.wrap(merkleTreeIndex));
    return tree.getRoot(index.intValue());
  }

  protected synchronized void chainStart(
      byte[] deposit_root, byte[] deposit_count, byte[] time, byte[] blockHash) {
    assert UInt64.fromBytesLittleEndian(Bytes8.wrap(deposit_count)).intValue()
        == initialDeposits.size();
    ChainStart chainStart =
        new ChainStart(
            Time.castFrom(UInt64.fromBytesLittleEndian(Bytes8.wrap(time))),
            new Eth1Data(
                Hash32.wrap(Bytes32.wrap(deposit_root)),
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

  private Deposit createUnProofedDeposit(DepositEventData eventData) {
    UInt64 index = UInt64.fromBytesLittleEndian(Bytes8.wrap(eventData.merkleTreeIndex));
    DepositData depositData =
        new DepositData(
            BLSPubkey.wrap(Bytes48.wrap(eventData.pubkey)),
            Hash32.wrap(Bytes32.wrap(eventData.withdrawalCredentials)),
            Gwei.castFrom(UInt64.fromBytesLittleEndian(Bytes8.wrap(eventData.amount))),
            BLSSignature.wrap(Bytes96.wrap(eventData.signature)));
    return new Deposit(ReadVector.wrap(Collections.emptyList(), Integer::new), index, depositData);
  }

  @Override
  public boolean hasDepositRoot(Hash32 blockHash, Hash32 depositRoot) {
    return hasDepositRootImpl(blockHash.extractArray(), depositRoot.extractArray());
  }

  protected abstract boolean hasDepositRootImpl(byte[] blockHash, byte[] depositRoot);

  @Override
  public Optional<Eth1Data> getLatestEth1Data() {
    return getLatestBlockHashDepositRoot()
        .map(
            r ->
                new Eth1Data(
                    Hash32.wrap(Bytes32.wrap(r.getValue0())),
                    UInt64.valueOf(r.getValue1()),
                    Hash32.wrap(Bytes32.wrap(r.getValue2()))));
  }

  protected abstract Optional<Triplet<byte[], Integer, byte[]>> getLatestBlockHashDepositRoot();

  @Override
  public List<DepositInfo> peekDeposits(
      int count, Eth1Data fromDepositExclusive, Eth1Data tillDepositInclusive) {
    return peekDepositsImpl(
            count,
            fromDepositExclusive.getBlockHash().extractArray(),
            tillDepositInclusive.getBlockHash().extractArray())
        .stream()
        .map(
            blockDepositPair ->
                restoreDeposits(blockDepositPair.getValue1(), blockDepositPair.getValue0()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  protected abstract List<Pair<byte[], List<DepositEventData>>> peekDepositsImpl(
      int count, byte[] startBlockHash, byte[] endBlockHash);

  protected long getDistanceFromHead() {
    return distanceFromHead;
  }

  @Override
  public void setDistanceFromHead(long distanceFromHead) {
    this.distanceFromHead = distanceFromHead;
  }

  protected class DepositEventData {
    public final byte[] pubkey;
    public final byte[] withdrawalCredentials;
    public final byte[] amount;
    public final byte[] signature;
    public final byte[] merkleTreeIndex;

    public DepositEventData(
        byte[] pubkey,
        byte[] withdrawalCredentials,
        byte[] amount,
        byte[] signature,
        byte[] merkleTreeIndex) {
      this.pubkey = pubkey;
      this.withdrawalCredentials = withdrawalCredentials;
      this.amount = amount;
      this.signature = signature;
      this.merkleTreeIndex = merkleTreeIndex;
    }
  }
}
