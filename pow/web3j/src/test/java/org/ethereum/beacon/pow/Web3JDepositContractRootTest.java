package org.ethereum.beacon.pow;

import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.schedulers.Schedulers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytes48;
import tech.pegasys.artemis.util.uint.UInt64;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * Checks that deposit root from contract and merkle tree matches each other
 *
 * <p>See {@link AbstractWeb3JTest} for prerequisites
 */
@Ignore("Takes several minutes to run")
public class Web3JDepositContractRootTest extends AbstractWeb3JTest {
  @Test
  public void testVerifyDepositRoot() throws Exception {
    AtomicLong latestProcessedBlock = new AtomicLong(0);
    byte[] latestCalculatedDepositRoot = new byte[32];
    Web3JDepositContract depositContract =
        new Web3JDepositContract(
            web3j,
            contract.getContractAddress(),
            contractDeployBlock,
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH,
            createSpecNoBls()) {

          // avoid async block processing
          @Override
          protected void processConfirmedBlocks() {
            long bestConfirmedBlock = getBestConfirmedBlock();
            processBlocksUpTo(bestConfirmedBlock);
            latestProcessedBlock.set(bestConfirmedBlock);
          }

          // store deposit root in latestCalculatedDepositRoot
          @Override
          protected synchronized void newDeposits(
              List<DepositEventData> eventDataList, byte[] blockHash, long blockTimestamp) {
            super.newDeposits(eventDataList, blockHash, blockTimestamp);
            for (DepositEventData eventData : eventDataList) {
              System.arraycopy(
                  getDepositRoot(eventData.index).extractArray(),
                  0,
                  latestCalculatedDepositRoot,
                  0,
                  32);
            }
          }
        };
    depositContract.setDistanceFromHead(3);
    // Fire contract handling
    Mono.from(depositContract.getChainStartMono())
        .subscribe(chainStart -> System.out.println("Chain started!"));

    // Check that contract deployed successfully and is empty
    final byte[] depositCount = contract.get_deposit_count().send();
    assertEquals(
        UInt64.ZERO, UInt64.fromBytesLittleEndian(Bytes8.rightPad(BytesValue.wrap(depositCount))));
    final byte[] hashTreeRoot = contract.get_hash_tree_root().send();
    assertEquals(
        Hash32.fromHexString("0xd70a234731285c6804c2a4f56711ddb8c82c99740f207854891028af34e27e5e"),
        Hash32.wrap(Bytes32.wrap(hashTreeRoot)));

    // Submit deposits from several validators
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

      // wait for block processing
      long depositBlock = receipt.getBlockNumber().longValue();
      while (depositBlock > latestProcessedBlock.get()) {
        Thread.sleep(200);
      }
      byte[] depositRoot = contract.get_hash_tree_root().send();
      Assert.assertArrayEquals(depositRoot, latestCalculatedDepositRoot);
      System.out.println(String.format("Validator #%s submitted its deposit", i));
    }

    ChainStart chainStart =
        Mono.from(depositContract.getChainStartMono()).block(Duration.ofSeconds(60));
    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(
          (byte) i, chainStart.getInitialDeposits().get(i).getData().getPubKey().get(0));
    }
  }
}
