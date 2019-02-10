package org.ethereum.beacon.pow;

import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.pow.validator.IncompleteSyncException;
import org.ethereum.beacon.pow.validator.TransactionBuilder;
import org.ethereum.core.CallTransaction;
import org.ethereum.core.Transaction;
import org.ethereum.crypto.ECKey;
import org.ethereum.facade.Ethereum;
import org.ethereum.facade.SyncStatus;
import org.ethereum.listener.RecommendedGasPriceTracker;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;

public class EthereumJTransactionBuilder implements TransactionBuilder {

  private final Ethereum ethereum;
  private final Address contractDeployAddress;
  private final CallTransaction.Contract contract;
  private final RecommendedGasPriceTracker gasPriceTracker;

  public EthereumJTransactionBuilder(Ethereum ethereum, String contractDeployAddress) {
    this.ethereum = ethereum;
    this.contractDeployAddress = Address.fromHexString(contractDeployAddress);
    this.contract = new CallTransaction.Contract(ContractAbi.getContractAbi());
    this.gasPriceTracker = new RecommendedGasPriceTracker();
    ethereum.addListener(gasPriceTracker);
  }

  @Override
  public boolean isReady() {
    return ethereum.getSyncStatus().getStage() != SyncStatus.SyncStage.Complete;
  }

  @Override
  public BytesValue createTransaction(String fromAddress, BytesValue depositInput, Gwei amount) {
    if (!(isReady())) {
      throw new IncompleteSyncException(
          "Unable to create transaction, because sync is not done yet. Query when sync is done.");
    }
    BigInteger nonce =
        ethereum.getRepository().getNonce(Address.fromHexString(fromAddress).getArrayUnsafe());
    byte[] data = contract.getByName("deposit").encode((Object) depositInput.getArrayUnsafe());
    Transaction tx =
        ethereum.createTransaction(
            nonce,
            BigInteger.valueOf(gasPriceTracker.getRecommendedGasPrice()),
            BigInteger.valueOf(2_000_000), // FIXME: Why this number??
            contractDeployAddress.getArrayUnsafe(),
            amount.weiValue(),
            data);
    return BytesValue.wrap(tx.getEncoded());
  }

  @Override
  public BytesValue signTransaction(BytesValue unsignedTransaction, BytesValue eth1PrivKey) {
    Transaction tx = new Transaction(unsignedTransaction.getArrayUnsafe());
    tx.sign(ECKey.fromPrivate(eth1PrivKey.getArrayUnsafe()));

    return BytesValue.wrap(tx.getEncoded());
  }
}
