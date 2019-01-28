package org.ethereum.beacon.pow;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.core.Bloom;
import org.ethereum.core.CallTransaction.Contract;
import org.ethereum.core.CallTransaction.Invocation;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.crypto.HashUtil;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.SyncStatus.SyncStage;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.vm.LogInfo;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;

public class EthereumJDepositContract extends AbstractDepositContract {
  private static final Logger logger = LoggerFactory.getLogger(EthereumJDepositContract.class);

  private static final String DEPOSIT_EVENT_NAME = "Deposit";
  private static final String CHAIN_START_EVENT_NAME = "ChainStart";

  private final ExecutorService blockExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("PowClientBlockProcessingThread").build());

  private final Ethereum ethereum;
  private final Address contractDeployAddress;
  private final Hash32 contractDeployAddressHash;
  private final long contractDeployBlock;
  private final Bloom contractAdderssBloom;
  private final Contract contract;

  private volatile long bestConfirmedBlock = 0;
  private volatile long processedUpToBlock;
  private boolean chainStartComplete;

  public EthereumJDepositContract(Ethereum ethereum, long contractDeployBlock,
      String contractDeployAddress) {
    this.ethereum = ethereum;
    this.contractDeployAddress = Address.fromHexString(contractDeployAddress);
    contractDeployAddressHash =
        Hash32.wrap(Bytes32.wrap(HashUtil.sha3(this.contractDeployAddress.extractArray())));
    this.contractAdderssBloom = Bloom.create(contractDeployAddressHash.extractArray());
    this.contract = new Contract(ContractAbi.getContractAbi());
    this.contractDeployBlock = contractDeployBlock;
    processedUpToBlock = contractDeployBlock;
  }

  @Override
  protected void chainStartSubscribed() {
    ethereum.addListener(new EthereumListenerAdapter() {
      @Override
      public void onSyncDone(SyncState state) {
        if (state == SyncState.COMPLETE) {
          onEthereumUpdated();
        }
      }

      @Override
      public void onBlock(Block block, List<TransactionReceipt> receipts) {
        onEthereumUpdated();
      }
    });

    if (ethereum.getSyncStatus().getStage() == SyncStage.Complete) {
      processConfirmedBlocks();
    }
  }

  @Override
  protected void chainStartDone() {
    chainStartComplete = true;
  }

  private void onEthereumUpdated() {
    if (!chainStartComplete) {
      processConfirmedBlocks();
    }
  }

  private void processConfirmedBlocks() {
    bestConfirmedBlock = ethereum.getBlockchain().getBestBlock().getNumber() - getDistanceFromHead();
    blockExecutor.submit(this::processBlocksUpTo);
  }

  private void processBlocksUpTo() {
    try {
      for (long number = processedUpToBlock; number < bestConfirmedBlock; number++) {
        Block block = ethereum.getBlockchain().getBlockByNumber(number);
        onConfirmedBlock(block);
        processedUpToBlock = number + 1;
      }
    } catch (Exception e) {
      logger.error("Error processing blocks: ", e);
    }
  }

  private List<TransactionReceipt> getBlockTransactionReceipts(Block block) {
    Blockchain blockchain = (Blockchain) ethereum.getBlockchain();
    return block.getTransactionsList().stream()
        .map(tx -> blockchain.getTransactionInfo(tx.getHash()).getReceipt())
        .collect(Collectors.toList());
  }

  private void onConfirmedBlock(Block block) {
    for (Invocation invocation : getContractEvents(block)) {
      if (DEPOSIT_EVENT_NAME.equals(invocation.function.name)) {
        newDeposit(createDepositEventData(invocation), block.getHash());
      } else if (CHAIN_START_EVENT_NAME.equals(invocation.function.name)) {
        chainStart((byte[]) invocation.args[0], (byte[]) invocation.args[1], block.getHash());
      } else {
        throw new IllegalStateException("Invalid event from the contract: " + invocation);
      }
    }
  }

  private DepositEventData createDepositEventData(Invocation depositEvent) {
    Object[] merkle_branch_obj = (Object[]) depositEvent.args[3];
    byte[][] merkle_branch_arr = new byte[merkle_branch_obj.length][];
    for (int i = 0; i < merkle_branch_obj.length; i++) {
      merkle_branch_arr[i] = (byte[]) merkle_branch_obj[i];
    }

    return new DepositEventData(
        (byte[]) depositEvent.args[0],
        (byte[]) depositEvent.args[1],
        (byte[]) depositEvent.args[2],
        merkle_branch_arr);
  }

  private List<Invocation> getContractEvents(Block block) {
    if (!new Bloom(block.getLogBloom()).matches(contractAdderssBloom)) {
      return Collections.emptyList();
    }
    List<Invocation> ret = new ArrayList<>();

    List<TransactionReceipt> receipts = getBlockTransactionReceipts(block);
    for (TransactionReceipt receipt : receipts) {
      for (LogInfo logInfo : receipt.getLogInfoList()) {
        if (Arrays.equals(logInfo.getAddress(), contractDeployAddress.getArrayUnsafe())) {
          ret.add(contract.parseEvent(logInfo));
        }
      }
    }

    return ret;
  }

  @Override
  protected boolean hasDepositRootImpl(byte[] blockHash, byte[] depositRoot) {
    Block block = ethereum.getBlockchain().getBlockByHash(blockHash);
    if (block == null) {
      return false;
    }
    if (ethereum.getBlockchain().getBestBlock().getNumber() - block.getNumber() < getDistanceFromHead()) {
      return false;
    }

    return getContractEvents(block).stream()
        .filter(invocation -> DEPOSIT_EVENT_NAME.equals(invocation.function.name))
        .anyMatch(invocation -> Arrays.equals((byte[]) invocation.args[0], depositRoot));
  }

  @Override
  protected Pair<byte[], byte[]> getLatestBlockHashDepositRoot() {
    long bestBlock = ethereum.getBlockchain().getBestBlock().getNumber() - getDistanceFromHead();
    for(long blockNum = bestBlock; blockNum >= contractDeployBlock; blockNum--) {
      Block block = ethereum.getBlockchain().getBlockByNumber(blockNum);
      List<Invocation> contractEvents = getContractEvents(block);
      Collections.reverse(contractEvents);
      for (Invocation contractEvent : contractEvents) {
        if (CHAIN_START_EVENT_NAME.equals(contractEvent.function.name)) {
          return Pair.with(block.getHash(), (byte[]) contractEvent.args[0]);
        } else {
          return Pair.with(block.getHash(), (byte[]) contractEvent.args[0]);
        }
      }
    }
    return null;
  }

  @Override
  protected List<Pair<byte[], DepositEventData>> peekDepositsImpl(int count, byte[] startBlockHash,
      byte[] startDepositRoot, byte[] endBlockHash, byte[] endDepositRoot) {
    List<Pair<byte[], DepositEventData>> ret = new ArrayList<>();
    Block startBlock = ethereum.getBlockchain().getBlockByHash(startBlockHash);
    Block endBlock = ethereum.getBlockchain().getBlockByHash(endBlockHash);

    Iterator<Pair<Block, DepositEventData>> iterator = iterateDepositEvents(startBlock, endBlock);
    boolean started = false;
    while (iterator.hasNext()) {
      if (Arrays.equals(startDepositRoot, iterator.next().getValue1().deposit_root)) {
        started = true;
        break;
      }
    }

    if (!started) {
      throw new IllegalStateException("Starting depositRoot not found");
    }

    while (iterator.hasNext() && count > 0) {
      Pair<Block, DepositEventData> event = iterator.next();
      ret.add(Pair.with(event.getValue0().getHash(), event.getValue1()));
      count--;
      if (Arrays.equals(endDepositRoot, event.getValue1().deposit_root)){
        break;
      }
    }

    return ret;
  }

  private Iterator<Pair<Block, DepositEventData>> iterateDepositEvents(Block fromInclusive,
      Block tillInclusive) {
    return new Iterator<Pair<Block, DepositEventData>>() {
      Iterator<Invocation> iterator = Collections.emptyIterator();
      Block curBlock;

      @Override
      public boolean hasNext() {
        while (!iterator.hasNext()) {
          if (curBlock == null) {
            curBlock = fromInclusive;
          } else {
            if (curBlock.getNumber() >= tillInclusive.getNumber()) {
              return false;
            }
            if (ethereum.getBlockchain().getBestBlock().getNumber() - getDistanceFromHead()
                <= curBlock.getNumber()) {
              return false;
            }
            curBlock = ethereum.getBlockchain().getBlockByNumber(curBlock.getNumber() + 1);
          }
          iterator =
              getContractEvents(curBlock)
                  .stream()
                  .filter(invocation -> DEPOSIT_EVENT_NAME.equals(invocation.function.name))
                  .iterator();
        }
        return true;
      }

      @Override
      public Pair<Block, DepositEventData> next() {
        return Pair.with(curBlock, createDepositEventData(iterator.next()));
      }
    };
  }
}
