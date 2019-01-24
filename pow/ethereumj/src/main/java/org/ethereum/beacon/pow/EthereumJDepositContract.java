package org.ethereum.beacon.pow;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.SyncStatus.SyncStage;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pegasys.artemis.util.bytes.Bytes32;

public class EthereumJDepositContract extends AbstractDepositContract {
  private static final Logger logger = LoggerFactory.getLogger(EthereumJDepositContract.class);

  private static final String DEPOSIT_EVENT_NAME = "Deposit";
  private static final String CHAIN_START_EVENT_NAME = "ChainStart";

  private final ExecutorService blockExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("PowClientBlockProcessingThread").build());

  private final Ethereum ethereum;
  private final Bytes32 contractDeployAddress;
  private final long contractDeployBlock;
  private final Bloom contractAdderssBloom;
  private final Contract contract;

  private volatile long bestConfirmedBlock = 0;
  private volatile long processedUpToBlock;
  private boolean chainStartComplete;

  public EthereumJDepositContract(Ethereum ethereum, long contractDeployBlock,
      String contractDeployAddress) {
    this.ethereum = ethereum;
    this.contractDeployAddress = Bytes32.fromHexString(contractDeployAddress);
    this.contractAdderssBloom = Bloom.create(this.contractDeployAddress.extractArray());
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
      for (long number = processedUpToBlock; number < bestConfirmedBlock; number++) {
        Block block = ethereum.getBlockchain().getBlockByNumber(number);
        onConfirmedBlock(block);
        processedUpToBlock = number + 1;
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
    return new DepositEventData(
        (byte[]) depositEvent.args[0],
        (byte[]) depositEvent.args[1],
        (byte[]) depositEvent.args[2],
        (byte[][]) depositEvent.args[3]);
  }

  private List<Invocation> getContractEvents(Block block) {
    if (!Bloom.create(block.getLogBloom()).matches(contractAdderssBloom)) {
      return Collections.emptyList();
    }
    List<Invocation> ret = new ArrayList<>();

    List<TransactionReceipt> receipts = getBlockTransactionReceipts(block);
    for (TransactionReceipt receipt : receipts) {
      for (LogInfo logInfo : receipt.getLogInfoList()) {
        if (logInfo.getTopics().contains(DataWord.of(contractDeployAddress.extractArray()))) {
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
    Block block = ethereum.getBlockchain().getBlockByHash(startBlockHash);

    boolean started = false;
    for (Invocation invocation : getContractEvents(block)) {
      if (DEPOSIT_EVENT_NAME.equals(invocation.function.name)) {
        DepositEventData eventData = createDepositEventData(invocation);
        if (!started) {
          if (Arrays.equals(startDepositRoot, eventData.deposit_root)){
            started = true;
          }
        } else {
          ret.add(Pair.with(block.getHash(), eventData));
          if (ret.size() == count) break;
        }
      }
    }

    if (!started) {
      throw new IllegalStateException("Starting depositRoot not found");
    }

    outer:
    while (true) {
      block = ethereum.getBlockchain().getBlockByNumber(block.getNumber() + 1);
      for (Invocation invocation : getContractEvents(block)) {
        if (DEPOSIT_EVENT_NAME.equals(invocation.function.name)) {
          DepositEventData eventData = createDepositEventData(invocation);
          ret.add(Pair.with(block.getHash(), eventData));
          if (Arrays.equals(endDepositRoot, eventData.deposit_root)
              || ret.size() == count){
            break outer;
          }
        }
      }
    }

    return ret;
  }
}
