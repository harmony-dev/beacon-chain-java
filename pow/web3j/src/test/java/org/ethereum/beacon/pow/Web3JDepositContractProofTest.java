package org.ethereum.beacon.pow;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.util.BlsKeyPairGenerator;
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
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks that deposit proofs are correct
 *
 * <p>See {@link AbstractWeb3JTest} for prerequisites
 */
@Ignore("Takes several minutes to run")
public class Web3JDepositContractProofTest extends AbstractWeb3JTest {
  @Test
  public void testVerifyProofs() throws Exception {
    BeaconChainSpec specWithVerify = createSpecVerifyProof();
    BlsKeyPairGenerator generator = BlsKeyPairGenerator.createWithoutSeed();
    List<Eth1Data> eth1DataList = new ArrayList<>();
    long lastDepositBlock = 0;
    for (int i = 0; i < 20; i++) {
      BLS381.KeyPair keyPair = generator.next();
      Bytes48 pubKey = keyPair.getPublic().getEncodedBytes();
      Hash32 withdrawalCredentials =
          Hash32.wrap(Bytes32.leftPad(UInt64.valueOf(i).toBytesBigEndian()));

      // Calculate signature
      DepositData depositData =
          new DepositData(
              BLSPubkey.wrap(pubKey),
              withdrawalCredentials,
              Gwei.castFrom(UInt64.valueOf(DEPOSIT_GWEI_AMOUNT.longValue())),
              BLSSignature.ZERO);
      MessageParameters messageParameters =
          MessageParameters.create(
              specWithVerify.signing_root(depositData), SignatureDomains.DEPOSIT);
      BLS381.Signature signature = BLS381.sign(messageParameters, keyPair);

      // Send deposit
      final TransactionReceipt receipt =
          contract
              .deposit(
                  pubKey.extractArray(),
                  withdrawalCredentials.extractArray(),
                  signature.getEncoded().extractArray(),
                  DEPOSIT_WEI_AMOUNT)
              .send();

      Assert.assertTrue(receipt.isStatusOK());
      Assert.assertEquals(1, receipt.getLogs().size());
      lastDepositBlock = receipt.getBlockNumber().longValue();

      // Store Eth1 for this deposit in list
      byte[] depositRoot = contract.get_hash_tree_root().send();
      byte[] depositCount = contract.get_deposit_count().send();
      Eth1Data lastDepositEthData =
          new Eth1Data(
              Hash32.wrap(Bytes32.wrap((depositRoot))),
              UInt64.fromBytesLittleEndian(Bytes8.wrap((depositCount))),
              Hash32.ZERO);
      eth1DataList.add(lastDepositEthData);

      System.out.println(
          String.format(
              "root %d: %s",
              lastDepositEthData.getDepositCount().decrement().getValue(),
              lastDepositEthData.getDepositRoot()));
    }

    // skip contract signature verification
    BeaconChainSpec specWithoutVerify = createSpecNoBls();
    Web3JDepositContract depositContract =
        new Web3JDepositContract(
            web3j,
            contract.getContractAddress(),
            contractDeployBlock,
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH,
            specWithoutVerify);
    depositContract.setDistanceFromHead(3);

    ChainStart chainStart =
        Mono.from(depositContract.getChainStartMono()).block(Duration.ofSeconds(60));
    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    MutableBeaconState beaconState = BeaconState.getEmpty().createMutableCopy();
    beaconState.setEth1Data(chainStart.getEth1Data());
    for (Deposit deposit : chainStart.getInitialDeposits()) {
      //       The proof for each deposit must be constructed against the deposit root contained in
      // state.latest_eth1_data rather than the deposit root at the time the deposit was initially
      // logged from the 1.0 chain. This entails storing a full deposit merkle tree locally and
      // computing updated proofs against the latest_eth1_data.deposit_root as needed. See
      // minimal_merkle.py for a sample implementation.
      specWithVerify.verify_deposit(beaconState, deposit);
      specWithVerify.process_deposit(beaconState, deposit);
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

    List<org.ethereum.beacon.pow.DepositContract.DepositInfo> depositInfos1 =
        depositContract.peekDeposits(100, chainStart.getEth1Data(), lastDepositEthData);

    // Verify remaining deposits
    Assert.assertEquals(4, depositInfos1.size());
    for (org.ethereum.beacon.pow.DepositContract.DepositInfo depositInfo : depositInfos1) {
      beaconState.setEth1Data(
          eth1DataList.get(depositInfo.getEth1Data().getDepositCount().decrement().intValue()));
      specWithVerify.verify_deposit(beaconState, depositInfo.getDeposit());
      specWithVerify.process_deposit(beaconState, depositInfo.getDeposit());
    }
  }
}
