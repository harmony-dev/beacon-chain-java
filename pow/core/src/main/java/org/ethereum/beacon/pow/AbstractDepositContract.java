package org.ethereum.beacon.pow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import tech.pegasys.artemis.util.uint.UInt64;

public abstract class AbstractDepositContract implements DepositContract {

  private static final int MAX_REORG_HEIGHT = 1000;

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
            .publishOn(this.schedulers.events().toReactor())
            .doOnSubscribe(s -> chainStartSubscribedPriv())
            .name("PowClient.chainStart");
    depositStream = new SimpleProcessor<>(this.schedulers.events(), "PowClient.deposit");
    this.tree = new DepositBufferedMerkle(hashFunction, treeDepth, MAX_REORG_HEIGHT);
  }

  /**
   * Stores deposits data from invocation list eventDataList
   *
   * @param eventDataList All deposit events in blockHash
   * @param blockHash Block hash
   */
  protected synchronized void newDeposits(List<DepositEventData> eventDataList, byte[] blockHash) {
    if (eventDataList.isEmpty()) {
      return;
    }

    List<DepositData> depositDataList =
        eventDataList.stream().map(this::createDepositData).collect(Collectors.toList());
    depositDataList.forEach(tree::addValue);

    int depositCount = eventDataList.get(eventDataList.size() - 1).getIndex().increment().intValue();
    for (DepositEventData data : eventDataList) {
      DepositData depositData = createDepositData(data);
      Deposit deposit =
          Deposit.create(tree.getProof(data.getIndex().intValue(), depositCount), depositData);

      if (startChainSubscribed && !chainStartSink.isTerminated()) {
        initialDeposits.add(deposit);
      }
      depositStream.onNext(deposit);
    }
  }

  /**
   * Same as {@link #newDeposits(List, byte[])} but doesn't store deposits data, instead expects its
   * already stored
   */
  private List<DepositInfo> restoreDeposits(List<DepositEventData> eventDataList, byte[] blockHash) {
    if (eventDataList.isEmpty()) {
      return Collections.emptyList();
    }

    List<DepositInfo> ret = new ArrayList<>();
    int depositCount = eventDataList.get(eventDataList.size() - 1).getIndex().increment().intValue();
    for (DepositEventData data : eventDataList) {

      DepositData depositData = createDepositData(data);
      List<Hash32> proof = tree.getProof(data.getIndex().getIntValue(), depositCount);
      Deposit deposit = Deposit.create(proof, depositData);

      Hash32 depositRoot = tree.getRoot(data.getIndex().getIntValue());
      Eth1Data eth1Data =
          new Eth1Data(
              depositRoot, data.getIndex().increment(), Hash32.wrap(Bytes32.wrap(blockHash)));

      ret.add(new DepositInfo(deposit, eth1Data));
    }

    return ret;
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

  private DepositData createDepositData(DepositEventData eventData) {
    return new DepositData(
        eventData.getPubkey(),
        eventData.getWithdrawalCredentials(),
        eventData.getAmount(),
        eventData.getSignature());
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
    public final byte[] index;

    public DepositEventData(
        byte[] pubkey,
        byte[] withdrawalCredentials,
        byte[] amount,
        byte[] signature,
        byte[] index) {
      this.pubkey = pubkey;
      this.withdrawalCredentials = withdrawalCredentials;
      this.amount = amount;
      this.signature = signature;
      this.index = index;
    }

    BLSPubkey getPubkey() {
      return BLSPubkey.wrap(Bytes48.wrap(pubkey));
    }

    Hash32 getWithdrawalCredentials() {
      return Hash32.wrap(Bytes32.wrap(withdrawalCredentials));
    }

    Gwei getAmount() {
      return Gwei.castFrom(UInt64.fromBytesLittleEndian(Bytes8.wrap(amount)));
    }

    UInt64 getIndex() {
      return UInt64.fromBytesLittleEndian(Bytes8.wrap(index));
    }

    BLSSignature getSignature() {
      return BLSSignature.wrap(Bytes96.wrap(signature));
    }
  }
}
