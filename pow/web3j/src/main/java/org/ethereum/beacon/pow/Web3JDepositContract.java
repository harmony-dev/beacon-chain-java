package org.ethereum.beacon.pow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.pow.contract.DepositContract;
import org.ethereum.beacon.pow.util.BloomFilter;
import org.ethereum.beacon.schedulers.LatestExecutor;
import org.ethereum.beacon.schedulers.Schedulers;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Web3JDepositContract extends AbstractDepositContract {
  private static final Logger logger = LogManager.getLogger(Web3JDepositContract.class);

  private final LatestExecutor<Long> blockExecutor;
  private final Web3j web3j;
  private final Web3RequestExecutor web3RequestExecutor;
  private final org.ethereum.beacon.pow.contract.DepositContract depositContract;
  private final Address contractDeployAddress;
  private final Hash32 contractDeployAddressHash;
  private final long contractDeployBlock;
  private final BloomFilter contractAddressBloom;
  private volatile long processedUpToBlock;
  private boolean chainStartComplete;

  public Web3JDepositContract(
      Web3j web3j,
      String contractDeployAddress,
      long contractDeployBlock,
      Schedulers schedulers,
      Function<BytesValue, Hash32> hashFunction,
      int merkleTreeDepth,
      BeaconChainSpec spec) {
    super(schedulers, hashFunction, merkleTreeDepth, spec);
    this.web3j = web3j;

    this.contractDeployAddress = Address.fromHexString(contractDeployAddress);
    contractDeployAddressHash =
        Hash32.wrap(Bytes32.wrap(Hash.sha3(this.contractDeployAddress.extractArray())));
    this.contractAddressBloom = BloomFilter.create(contractDeployAddressHash.extractArray());
    this.contractDeployBlock = contractDeployBlock;
    this.depositContract =
        org.ethereum.beacon.pow.contract.DepositContract.load(
            contractDeployAddress,
            web3j,
            Credentials.create(ECKeyPair.create(BigInteger.ONE)),
            new DefaultGasProvider());

    processedUpToBlock = contractDeployBlock;
    blockExecutor = new LatestExecutor<>(this.schedulers.blocking(), this::processBlocksUpTo);
    this.web3RequestExecutor = new Web3RequestExecutor(web3j);
  }

  @Override
  protected void chainStartSubscribed() {
    web3RequestExecutor.executeOnSyncDone(
        () -> {
          processConfirmedBlocks();
          onEthereumUpdated();
        });
    web3j
        .blockFlowable(false)
        .doOnNext(
            block -> {
              onEthereumUpdated();
            })
        .subscribe();
  }

  @Override
  protected void chainStartDone() {
    chainStartComplete = true;
  }

  private void onEthereumUpdated() {
    processConfirmedBlocks();
  }

  long getBestConfirmedBlock() {
    return web3j.ethBlockNumber().sendAsync().join().getBlockNumber().longValue()
        - getDistanceFromHead();
  }

  void processConfirmedBlocks() {
    long bestConfirmedBlock = getBestConfirmedBlock();
    blockExecutor.newEvent(bestConfirmedBlock);
  }

  void processBlocksUpTo(long bestConfirmedBlock) {
    for (long number = processedUpToBlock; number <= bestConfirmedBlock; number++) {
      EthBlock block =
          web3j
              .ethGetBlockByNumber(DefaultBlockParameter.valueOf(BigInteger.valueOf(number)), false)
              .sendAsync()
              .join();
      onConfirmedBlock(block.getBlock());
      processedUpToBlock = number + 1;
    }
  }

  private Optional<TransactionReceipt> getReceiptFor(EthBlock.TransactionResult txResult) {
    CompletableFuture<EthGetTransactionReceipt> future;
    if (txResult instanceof EthBlock.TransactionHash) {
      future =
          web3j.ethGetTransactionReceipt(((EthBlock.TransactionHash) txResult).get()).sendAsync();
    } else if (txResult instanceof EthBlock.TransactionObject) {
      future =
          web3j
              .ethGetTransactionReceipt(((EthBlock.TransactionObject) txResult).get().getHash())
              .sendAsync();
    } else {
      throw new RuntimeException("txResult type not supported for txResult: " + txResult);
    }

    return future.thenApply(EthGetTransactionReceipt::getTransactionReceipt).join();
  }

  private List<TransactionReceipt> getBlockTransactionReceipts(EthBlock.Block block) {
    return block.getTransactions().stream()
        .map(this::getReceiptFor)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private synchronized void onConfirmedBlock(EthBlock.Block block) {
    List<DepositEventData> depositEventDataList = getDepositEvents(block);
    if (!depositEventDataList.isEmpty()) {
      newDeposits(
          depositEventDataList,
          Numeric.hexStringToByteArray(block.getHash()),
          block.getTimestamp().longValue());
    }
  }

  private DepositEventData createDepositEventData(DepositContract.DepositEventEventResponse event) {
    return new DepositEventData(
        event.pubkey, event.withdrawal_credentials, event.amount, event.signature, event.index);
  }

  private List<DepositEventData> getDepositEvents(EthBlock.Block block) {
    if (!new BloomFilter(Numeric.hexStringToByteArray(block.getLogsBloom()))
        .matches(contractAddressBloom)) {
      return Collections.emptyList();
    }

    List<DepositEventData> ret = new ArrayList<>();
    List<TransactionReceipt> receipts = getBlockTransactionReceipts(block);
    for (TransactionReceipt receipt : receipts) {
      List<DepositContract.DepositEventEventResponse> responses =
          depositContract.getDepositEventEvents(receipt);
      List<DepositEventData> depositEvents =
          responses.stream().map(this::createDepositEventData).collect(Collectors.toList());
      ret.addAll(depositEvents);
    }

    return ret;
  }

  @Override
  protected boolean hasDepositRootImpl(byte[] blockHash, byte[] depositRoot) {
    EthBlock.Block block =
        web3j
            .ethGetBlockByHash(Numeric.toHexString(blockHash), false)
            .sendAsync()
            .join()
            .getBlock();
    if (block == null || block.getNumber().longValue() > getBestConfirmedBlock()) {
      return false;
    }

    return getDepositEvents(block).stream()
        .anyMatch(depositEvent -> Arrays.equals(depositEvent.pubkey, depositRoot));
  }

  @Override
  protected synchronized Optional<Triplet<byte[], Integer, byte[]>>
      getLatestBlockHashDepositRoot() {
    long bestBlock = getBestConfirmedBlock();
    for (long blockNum = bestBlock; blockNum >= contractDeployBlock; blockNum--) {
      EthBlock.Block block =
          web3j
              .ethGetBlockByNumber(
                  DefaultBlockParameter.valueOf(BigInteger.valueOf(blockNum)), false)
              .sendAsync()
              .join()
              .getBlock();
      List<DepositEventData> contractEvents = getDepositEvents(block);
      Collections.reverse(contractEvents);
      for (DepositEventData eventData : contractEvents) {
        return Optional.of(
            Triplet.with(
                getDepositRoot(eventData.index).extractArray(),
                UInt64.fromBytesLittleEndian(Bytes8.wrap(eventData.index)).increment().intValue(),
                Numeric.hexStringToByteArray(block.getHash())));
      }
    }

    return Optional.empty();
  }

  @Override
  protected List<Pair<byte[], List<DepositEventData>>> peekDepositsImpl(
      int count, byte[] startBlockHash, byte[] endBlockHash) {
    List<Pair<byte[], List<DepositEventData>>> ret = new ArrayList<>();

    CompletableFuture<EthBlock> startBlockFut =
        web3j.ethGetBlockByHash(Numeric.toHexString(startBlockHash), false).sendAsync();
    CompletableFuture<EthBlock> endBlockFut =
        web3j.ethGetBlockByHash(Numeric.toHexString(endBlockHash), false).sendAsync();
    CompletableFuture.allOf(startBlockFut, endBlockFut).join();

    Iterator<Pair<EthBlock.Block, List<DepositEventData>>> iterator =
        iterateDepositEvents(startBlockFut.join().getBlock(), endBlockFut.join().getBlock());
    boolean started = false;
    while (iterator.hasNext()) {
      if (Arrays.equals(
          startBlockHash, Numeric.hexStringToByteArray(iterator.next().getValue0().getHash()))) {
        started = true;
        break;
      }
    }

    if (!started) {
      return ret;
    }

    while (iterator.hasNext() && count > 0) {
      Pair<EthBlock.Block, List<DepositEventData>> event = iterator.next();
      ret.add(
          Pair.with(Numeric.hexStringToByteArray(event.getValue0().getHash()), event.getValue1()));
      count--;
      if (Arrays.equals(endBlockHash, Numeric.hexStringToByteArray(event.getValue0().getHash()))) {
        break;
      }
    }

    return ret;
  }

  private Iterator<Pair<EthBlock.Block, List<DepositEventData>>> iterateDepositEvents(
      EthBlock.Block fromInclusive, EthBlock.Block tillInclusive) {
    return new Iterator<Pair<EthBlock.Block, List<DepositEventData>>>() {
      Iterator<List<DepositEventData>> iterator = Collections.emptyIterator();
      EthBlock.Block curBlock;

      @Override
      public boolean hasNext() {
        while (!iterator.hasNext()) {
          if (curBlock == null) {
            curBlock = fromInclusive;
          } else {
            if (curBlock.getNumber().longValue() >= tillInclusive.getNumber().longValue()) {
              return false;
            }
            if (getBestConfirmedBlock() <= curBlock.getNumber().longValue()) {
              return false;
            }
            curBlock =
                web3j
                    .ethGetBlockByNumber(
                        DefaultBlockParameter.valueOf(curBlock.getNumber().add(BigInteger.ONE)),
                        false)
                    .sendAsync()
                    .join()
                    .getBlock();
          }
          List<DepositEventData> cur = getDepositEvents(curBlock);
          if (!cur.isEmpty()) {
            List<List<DepositEventData>> iteratorList = new ArrayList<>();
            iteratorList.add(cur);
            iterator = iteratorList.iterator();
          }
        }
        return true;
      }

      @Override
      public Pair<EthBlock.Block, List<DepositEventData>> next() {
        return Pair.with(curBlock, iterator.next());
      }
    };
  }
}
