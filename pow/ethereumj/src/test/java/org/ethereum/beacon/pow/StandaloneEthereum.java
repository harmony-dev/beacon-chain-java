package org.ethereum.beacon.pow;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.ethereum.config.CommonConfig;
import org.ethereum.config.SystemProperties;
import org.ethereum.core.Block;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Repository;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.facade.Blockchain;
import org.ethereum.facade.EthereumImpl;
import org.ethereum.listener.CompositeEthereumListener;
import org.ethereum.listener.EthereumListener;
import org.ethereum.listener.EthereumListenerAdapter;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.ethereum.vm.program.ProgramResult;

public class StandaloneEthereum extends EthereumImpl {
  StandaloneBlockchain standaloneBlockchain;
  CompositeEthereumListener listener;

  public StandaloneEthereum(
      SystemProperties config,
      StandaloneBlockchain standaloneBlockchain) {
    super(config, getListenerHack(standaloneBlockchain));
    this.standaloneBlockchain = standaloneBlockchain;
  }

  private static CompositeEthereumListener getListenerHack(StandaloneBlockchain sb) {
    try {
      Field field = sb.getClass().getField("listener");
      field.setAccessible(true);
      return (CompositeEthereumListener) field.get(sb);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public org.ethereum.facade.Repository getPendingState() {
    return standaloneBlockchain.getPendingState().getRepository();
  }

  @Override
  public Future<Transaction> submitTransaction(Transaction transaction) {
    standaloneBlockchain.submitTransaction(transaction);
    CompletableFuture<Transaction> ret = new CompletableFuture<>();
    ret.complete(transaction);
    return ret;
  }

  @Override
  public ProgramResult callConstantFunction(
      String receiveAddress,
      ECKey senderPrivateKey,
      CallTransaction.Function function,
      Object... funcArgs) {
    Transaction tx =
        CallTransaction.createCallTransaction(
            0, 0, 100000000000000L, receiveAddress, 0, function, funcArgs);
    tx.sign(senderPrivateKey);
    Block block = standaloneBlockchain.getBlockchain().getBestBlock();

    Repository repository =
        standaloneBlockchain
            .getBlockchain()
            .getRepository()
            .getSnapshotTo(block.getStateRoot())
            .startTracking();

    try {
      org.ethereum.core.TransactionExecutor executor =
          new org.ethereum.core.TransactionExecutor(
                  tx,
                  block.getCoinbase(),
                  repository,
                  standaloneBlockchain.getBlockchain().getBlockStore(),
                  standaloneBlockchain.getBlockchain().getProgramInvokeFactory(),
                  block,
                  new EthereumListenerAdapter(),
                  0)
              .withCommonConfig(CommonConfig.getDefault())
              .setLocalCall(true);

      executor.init();
      executor.execute();
      executor.go();
      executor.finalization();

      return executor.getResult();
    } finally {
      repository.rollback();
    }
  }

  @Override
  public org.ethereum.facade.Repository getRepository() {
    return standaloneBlockchain.getBlockchain().getRepository();
  }

  @Override
  public void addListener(EthereumListener listener) {
    standaloneBlockchain.addEthereumListener(listener);
  }

  @Override
  public Blockchain getBlockchain() {
    return standaloneBlockchain.getBlockchain();
  }
}
