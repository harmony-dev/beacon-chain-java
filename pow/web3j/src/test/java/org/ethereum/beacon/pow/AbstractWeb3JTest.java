package org.ethereum.beacon.pow;

import io.reactivex.disposables.Disposable;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.pow.contract.DepositContract;
import org.junit.Before;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Precondition: Start configured geth docker from "geth" directory in resources. It contains
 * pre-mine for ALICE and required network settings to run deposit contract.
 */
public abstract class AbstractWeb3JTest {
  private static final Credentials ALICE =
      Credentials.create(
          Hash.sha3(BytesValue.wrap("cow".getBytes()).toString()) // premined in genesis
          );
  final int MERKLE_TREE_DEPTH =
      BeaconChainSpec.DEFAULT_CONSTANTS.getDepositContractTreeDepth().intValue();
  final Function<BytesValue, Hash32> HASH_FUNCTION = Hashes::sha256;
  BigInteger DEPOSIT_GWEI_AMOUNT = BigInteger.valueOf(32L * 1_000_000_000L);
  BigInteger DEPOSIT_WEI_AMOUNT =
      Convert.toWei(new BigDecimal(DEPOSIT_GWEI_AMOUNT), Convert.Unit.GWEI).toBigInteger();
  Admin web3j;
  DepositContract contract;
  Long contractDeployBlock;

  AbstractWeb3JTest() {}

  BeaconChainSpec createSpecNoBls() {
    return new BeaconChainSpec.Builder()
        .withDefaultHashFunction()
        .withVerifyDepositProof(false)
        .withBlsVerifyProofOfPossession(false)
        .withConstants(
            new SpecConstants() {
              @Override
              public UInt64 getMinGenesisActiveValidatorCount() {
                return UInt64.valueOf(16);
              }

              @Override
              public Time getSecondsPerDay() {
                return Time.of(5);
              }

              @Override
              public Time getMinGenesisTime() {
                return Time.of(0);
              }
            })
        .withDefaultHasher()
        .build();
  }

  BeaconChainSpec createSpecVerifyProof() {
    return new BeaconChainSpec.Builder()
        .withDefaultHashFunction()
        .withVerifyDepositProof(true)
        .withBlsVerifyProofOfPossession(true)
        .withConstants(
            new SpecConstants() {
              @Override
              public UInt64 getMinGenesisActiveValidatorCount() {
                return UInt64.valueOf(16);
              }

              @Override
              public Time getSecondsPerDay() {
                return Time.of(5);
              }

              @Override
              public Time getMinGenesisTime() {
                return Time.of(0);
              }
            })
        .withDefaultHasher()
        .build();
  }

  @Before
  public void setUp() throws Exception {
    this.web3j = Admin.build(new HttpService());
    Map<String, Long> allTxs = new HashMap<>();
    Disposable newBlockSub =
        web3j
            .blockFlowable(true)
            .subscribe(
                block -> {
                  for (EthBlock.TransactionResult txResult : block.getBlock().getTransactions()) {
                    Transaction tx = ((EthBlock.TransactionObject) txResult.get()).get();
                    allTxs.put(tx.getHash(), block.getBlock().getNumber().longValue());
                  }
                });
    this.contract = DepositContract.deploy(web3j, ALICE, new DefaultGasProvider()).send();
    String txHash = contract.getTransactionReceipt().get().getTransactionHash();
    while (!allTxs.containsKey(txHash)) {
      Thread.sleep(500);
    }
    this.contractDeployBlock = allTxs.get(txHash);
    System.out.println("Contract deployed by tx in block #" + contractDeployBlock);
    newBlockSub.dispose();
  }
}
