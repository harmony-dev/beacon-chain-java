package org.ethereum.beacon.pow;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.ethereum.core.Block;
import org.ethereum.core.Blockchain;
import org.ethereum.core.Bloom;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.CallTransaction.Contract;
import org.ethereum.core.CallTransaction.Invocation;
import org.ethereum.core.Transaction;
import org.ethereum.core.TransactionReceipt;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.SyncStatus.SyncStage;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.vm.DataWord;
import org.ethereum.vm.LogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pegasys.artemis.util.bytes.Bytes32;

public class EthereumJDepositContract extends AbstractDepositContract {
  private static final Logger logger = LoggerFactory.getLogger(EthereumJDepositContract.class);

  private static final String DEPOSIT_EVENT_NAME = "Deposit";
  private static final String CHAIN_START_EVENT_NAME = "ChainStart";

  private final ExecutorService blockExecutor = Executors.newSingleThreadExecutor(
      new ThreadFactoryBuilder().setNameFormat("PowClientBlockProcessingThread").build());

  private long blockConfirmations = 16;
  private final Ethereum ethereum;
  private final Bytes32 contractDeployAddress;
  private final Bloom contractAdderssBloom;
  private final Contract contract;

  private volatile long bestConfirmedBlock = 0;
  private volatile long processedUpToBlock;

  public EthereumJDepositContract(Ethereum ethereum, long contractDeployBlock,
      String contractDeployAddress) {
    this.ethereum = ethereum;
    this.contractDeployAddress = Bytes32.fromHexString(contractDeployAddress);
    this.contractAdderssBloom = Bloom.create(this.contractDeployAddress.extractArray());
    this.contract = new Contract(ContractAbi.getContractAbi());
    processedUpToBlock = contractDeployBlock;
  }

  public EthereumJDepositContract withBlockConfirmations(long blockConfirmations) {
    this.blockConfirmations = blockConfirmations;
    return this;
  }

  @Override
  protected void chainStartSubscribed() {
    start();
  }

  @Override
  protected void depositsSubscribed(byte[] fromBlockHash) {

  }

  @Override
  protected void depositsUnsubscribed() {

  }

  public void start() {
    ethereum.addListener(new EthereumListenerAdapter() {
      @Override
      public void onSyncDone(SyncState state) {
        if (state == SyncState.COMPLETE) {
          processConfirmedBlocks();
        }
      }

      @Override
      public void onBlock(Block block, List<TransactionReceipt> receipts) {
        processConfirmedBlocks();
      }
    });

    if (ethereum.getSyncStatus().getStage() == SyncStage.Complete) {
      processConfirmedBlocks();
    }
  }

  private void processConfirmedBlocks() {
    bestConfirmedBlock = ethereum.getBlockchain().getBestBlock().getNumber() - blockConfirmations;
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
        newDeposit(
            (byte[]) invocation.args[0],
            (byte[]) invocation.args[1],
            (byte[]) invocation.args[2],
            (byte[][]) invocation.args[3],
            block.getHash());
      } else if (CHAIN_START_EVENT_NAME.equals(invocation.function.name)) {
        chainStart((byte[]) invocation.args[0], (byte[]) invocation.args[1], block.getHash());
      } else {
        throw new IllegalStateException("Invalid event from the contract: " + invocation);
      }
    }
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
    if (ethereum.getBlockchain().getBestBlock().getNumber() - block.getNumber() < blockConfirmations) {
      return false;
    }

    return getContractEvents(block).stream()
        .filter(invocation -> DEPOSIT_EVENT_NAME.equals(invocation.function.name))
        .anyMatch(invocation -> Arrays.equals((byte[]) invocation.args[0], depositRoot));
  }
}
