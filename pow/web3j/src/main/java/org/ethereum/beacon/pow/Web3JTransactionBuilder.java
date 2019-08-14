package org.ethereum.beacon.pow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.pow.validator.TransactionBuilder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Convert;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.ethereum.beacon.pow.contract.DepositContract.FUNC_DEPOSIT;

public class Web3JTransactionBuilder implements TransactionBuilder {
  private static final Logger logger = LogManager.getLogger(Web3JTransactionBuilder.class);
  private final Web3j web3j;
  private final Address contractDeployAddress;
  private final Web3RequestExecutor web3RequestExecutor;

  public Web3JTransactionBuilder(Web3j web3j, String contractDeployAddress) {
    this.web3j = web3j;
    this.contractDeployAddress = Address.fromHexString(contractDeployAddress);
    this.web3RequestExecutor = new Web3RequestExecutor(web3j);
  }

  @Override
  public CompletableFuture<BytesValue> createTransaction(
      String fromAddress, DepositData depositData, Gwei amount) {
    CompletableFuture<BytesValue> result = new CompletableFuture<>();
    web3RequestExecutor.executeOnSyncDone(
        () -> {
          CompletableFuture<EthGetTransactionCount> txCountFut =
              web3j
                  .ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                  .sendAsync();
          CompletableFuture<EthGasPrice> gasPriceFut = web3j.ethGasPrice().sendAsync();
          CompletableFuture.allOf(txCountFut, gasPriceFut)
              .thenAccept(
                  aVoid -> {
                    BigInteger nonce = txCountFut.join().getTransactionCount();
                    BigInteger value =
                        Convert.toWei(amount.longValue() + "", Convert.Unit.GWEI).toBigInteger();
                    BigInteger gasLimit = BigInteger.valueOf(2_000_000);
                    final Function function =
                        new Function(
                            FUNC_DEPOSIT,
                            Arrays.asList(
                                new org.web3j.abi.datatypes.DynamicBytes(
                                    depositData.getPubKey().extractArray()),
                                new org.web3j.abi.datatypes.DynamicBytes(
                                    depositData.getWithdrawalCredentials().extractArray()),
                                new org.web3j.abi.datatypes.DynamicBytes(
                                    depositData.getSignature().extractArray())),
                            Collections.emptyList());
                    String encodedFunction = FunctionEncoder.encode(function);
                    byte[] rawTransaction =
                        RlpEncoder.encode(
                            new RlpList(
                                RlpString.create(nonce),
                                RlpString.create(gasPriceFut.join().getGasPrice()),
                                RlpString.create(gasLimit),
                                RlpString.create(contractDeployAddress.toString()),
                                RlpString.create(value),
                                RlpString.create(encodedFunction)));
                    result.complete(BytesValue.wrap(rawTransaction));
                  });
        });

    return result;
  }

  @Override
  public CompletableFuture<BytesValue> signTransaction(
      BytesValue unsignedTransaction, BytesValue eth1PrivKey) {
    CompletableFuture<BytesValue> result = new CompletableFuture<>();
    web3RequestExecutor.executeOnSyncDone(
        () -> {
          RlpList list = RlpDecoder.decode(unsignedTransaction.extractArray());
          ECKeyPair keyPair = ECKeyPair.create(eth1PrivKey.extractArray());
          // by client, so this module provides an opportunity to
          // manage private key by 3rd party application
          RawTransaction rawTransaction =
              RawTransaction.createTransaction(
                  ((RlpString) list.getValues().get(0)).asPositiveBigInteger(), // nonce
                  ((RlpString) list.getValues().get(1)).asPositiveBigInteger(), // gas price
                  ((RlpString) list.getValues().get(2)).asPositiveBigInteger(), // gas limit
                  ((RlpString) list.getValues().get(3)).asString(), // contract address
                  ((RlpString) list.getValues().get(4)).asPositiveBigInteger(), // amount
                  ((RlpString) list.getValues().get(5)).asString() // data
                  );
          byte[] signedTx =
              TransactionEncoder.signMessage(rawTransaction, Credentials.create(keyPair));
          result.complete(BytesValue.wrap(signedTx));
        });

    return result;
  }
}
