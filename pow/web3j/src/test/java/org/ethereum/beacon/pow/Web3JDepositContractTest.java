package org.ethereum.beacon.pow;

import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytes48;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * General test: submit 20 deposits, chain starts on 16, checks that contract handled it as it
 * should
 *
 * <p>See {@link AbstractWeb3JTest} for prerequisites
 */
@Ignore("Takes several minutes to run")
public class Web3JDepositContractTest extends AbstractWeb3JTest {
  @Test
  public void test1() throws Exception {
    // Check that contract deployed successfully and is empty
    final byte[] depositCount = contract.get_deposit_count().send();
    assertEquals(
        UInt64.ZERO, UInt64.fromBytesLittleEndian(Bytes8.rightPad(BytesValue.wrap(depositCount))));
    final byte[] hashTreeRoot = contract.get_hash_tree_root().send();
    assertEquals(
        Hash32.fromHexString("0xd70a234731285c6804c2a4f56711ddb8c82c99740f207854891028af34e27e5e"),
        Hash32.wrap(Bytes32.wrap(hashTreeRoot)));

    // Submit deposits from several validators
    long lastDepositBlock = 0;
    for (int i = 0; i < 20; i++) {
      MutableBytes48 pubKey = MutableBytes48.create();
      pubKey.set(0, (byte) i);

      final TransactionReceipt receipt =
          contract
              .deposit(
                  pubKey.extractArray(),
                  Hash32.ZERO.extractArray(),
                  Bytes96.ZERO.extractArray(),
                  DEPOSIT_WEI_AMOUNT)
              .send();

      Assert.assertTrue(receipt.isStatusOK());
      Assert.assertEquals(1, receipt.getLogs().size());
      lastDepositBlock = receipt.getBlockNumber().longValue();
      System.out.println(String.format("Validator #%s submitted its deposit", i));
    }

    Web3JDepositContract depositContract =
        new Web3JDepositContract(
            web3j,
            contract.getContractAddress(),
            contractDeployBlock,
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH,
            createSpecNoBls());
    depositContract.setDistanceFromHead(3);

    ChainStart chainStart =
        Mono.from(depositContract.getChainStartMono()).block(Duration.ofSeconds(60));

    // 16 deposits until chain start according to spec
    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(
          (byte) i, chainStart.getInitialDeposits().get(i).getData().getPubKey().get(0));
    }

    // wait until block lastValidatorDeposit + 5 appears in network
    EthBlock.Block eth1Block = null;
    while (eth1Block == null) {
      eth1Block =
          web3j
              .ethGetBlockByNumber(
                  DefaultBlockParameter.valueOf(BigInteger.valueOf(lastDepositBlock + 5)), false)
              .send()
              .getBlock();
      if (eth1Block == null) {
        Thread.sleep(200);
      }
    }

    byte[] depositRoot = contract.get_hash_tree_root().send();
    Eth1Data lastDepositEthData =
        new Eth1Data(
            Hash32.wrap(Bytes32.wrap(depositRoot)),
            UInt64.ZERO,
            Hash32.wrap(Bytes32.fromHexString(eth1Block.getHash())));

    // we have 4 after chain start, so we are able to get 2
    List<org.ethereum.beacon.pow.DepositContract.DepositInfo> depositInfos1 =
        depositContract.peekDeposits(2, chainStart.getEth1Data(), lastDepositEthData);
    Assert.assertEquals(2, depositInfos1.size());
    Assert.assertEquals((byte) 16, depositInfos1.get(0).getDeposit().getData().getPubKey().get(0));
    Assert.assertEquals((byte) 17, depositInfos1.get(1).getDeposit().getData().getPubKey().get(0));

    // and another 2. and that's all
    List<org.ethereum.beacon.pow.DepositContract.DepositInfo> depositInfos2 =
        depositContract.peekDeposits(200, depositInfos1.get(1).getEth1Data(), lastDepositEthData);
    Assert.assertEquals(2, depositInfos2.size());
    Assert.assertEquals((byte) 18, depositInfos2.get(0).getDeposit().getData().getPubKey().get(0));
    Assert.assertEquals((byte) 19, depositInfos2.get(1).getDeposit().getData().getPubKey().get(0));

    List<org.ethereum.beacon.pow.DepositContract.DepositInfo> depositInfos3 =
        depositContract.peekDeposits(200, lastDepositEthData, lastDepositEthData);
    Assert.assertEquals(0, depositInfos3.size());
  }
}
