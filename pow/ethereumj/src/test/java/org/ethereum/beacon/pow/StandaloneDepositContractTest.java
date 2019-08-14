package org.ethereum.beacon.pow;

import org.apache.commons.codec.binary.Hex;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.util.BlsKeyPairGenerator;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.config.SystemProperties;
import org.ethereum.facade.Ethereum;
import org.ethereum.solidity.compiler.CompilationResult.ContractMetadata;
import org.ethereum.util.blockchain.SolidityCallResult;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.ethereum.util.blockchain.StandaloneBlockchain.SolidityContractImpl;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytes48;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Ignore
public class StandaloneDepositContractTest {

  // modified spec constants:
  // GENESIS_ACTIVE_VALIDATOR_COUNT = 16  # 2**14
  // SECONDS_PER_DAY = 5

  final int MERKLE_TREE_DEPTH =
      BeaconChainSpec.DEFAULT_CONSTANTS.getDepositContractTreeDepth().intValue();
  final Function<BytesValue, Hash32> HASH_FUNCTION = Hashes::sha256;
  final String depositBin = ContractSource.getContractBin();
  BigInteger gweiAmount = BigInteger.valueOf(32L * 1_000_000_000L);
  BigInteger depositAmount = gweiAmount.multiply(BigInteger.valueOf(1_000_000_000L));

  private BeaconChainSpec createSpec() {
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

  @Test
  public void test1() {
    StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractSource.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    Object[] depositRoot = contract.callConstFunction("get_hash_tree_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    for (int i = 0; i < 20; i++) {
      MutableBytes48 pubKey = MutableBytes48.create();
      pubKey.set(0, (byte) i);

      SolidityCallResult result =
          contract.callFunction(
              depositAmount,
              "deposit",
              pubKey.extractArray(),
              Hash32.ZERO.extractArray(),
              Bytes96.ZERO.extractArray());

      Assert.assertTrue(result.isSuccessful());
      Assert.assertEquals(1, result.getEvents().size());
    }

    for (int i = 0; i < 16; i++) {
      sb.createBlock();
    }

    Ethereum ethereum = new StandaloneEthereum(new SystemProperties(), sb);
    EthereumJDepositContract depositContract =
        new EthereumJDepositContract(
            ethereum,
            0,
            BytesValue.wrap(contract.getAddress()).toString(),
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH,
            createSpec());
    depositContract.setDistanceFromHead(3);

    ChainStart chainStart =
        Mono.from(depositContract.getChainStartMono()).block(Duration.ofSeconds(60));

    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    Assert.assertEquals(
        17,
        sb.getBlockchain()
            .getBlockByHash(chainStart.getEth1Data().getBlockHash().extractArray())
            .getNumber());
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(
          (byte) i, chainStart.getInitialDeposits().get(i).getData().getPubKey().get(0));
    }

    depositRoot = contract.callConstFunction("get_hash_tree_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    Eth1Data lastDepositEthData =
        new Eth1Data(
            Hash32.wrap(Bytes32.wrap((byte[]) depositRoot[0])),
            UInt64.ZERO,
            Hash32.wrap(Bytes32.wrap(sb.getBlockchain().getBlockByNumber(21).getHash())));

    List<DepositInfo> depositInfos1 =
        depositContract.peekDeposits(2, chainStart.getEth1Data(), lastDepositEthData);

    Assert.assertEquals(2, depositInfos1.size());
    Assert.assertEquals((byte) 16, depositInfos1.get(0).getDeposit().getData().getPubKey().get(0));
    Assert.assertEquals((byte) 17, depositInfos1.get(1).getDeposit().getData().getPubKey().get(0));

    List<DepositInfo> depositInfos2 =
        depositContract.peekDeposits(200, depositInfos1.get(1).getEth1Data(), lastDepositEthData);

    Assert.assertEquals(2, depositInfos2.size());
    Assert.assertEquals((byte) 18, depositInfos2.get(0).getDeposit().getData().getPubKey().get(0));
    Assert.assertEquals((byte) 19, depositInfos2.get(1).getDeposit().getData().getPubKey().get(0));

    List<DepositInfo> depositInfos3 =
        depositContract.peekDeposits(200, lastDepositEthData, lastDepositEthData);
    Assert.assertEquals(0, depositInfos3.size());
  }

  @Test
  public void testOnline() {
    StandaloneBlockchain sb = new StandaloneBlockchain();
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractSource.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    sb.createBlock();

    Ethereum ethereum = new StandaloneEthereum(new SystemProperties(), sb);
    EthereumJDepositContract depositContract =
        new EthereumJDepositContract(
            ethereum,
            0,
            BytesValue.wrap(contract.getAddress()).toString(),
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH,
            createSpec());
    depositContract.setDistanceFromHead(3);
    Mono<ChainStart> chainStartMono = Mono.from(depositContract.getChainStartMono());
    chainStartMono.subscribe();

    for (int i = 0; i < 16; i++) {
      sb.createBlock();
    }

    for (int i = 0; i < 16; i++) {
      MutableBytes48 pubKey = MutableBytes48.create();
      pubKey.set(0, (byte) i);

      SolidityCallResult result =
          contract.callFunction(
              depositAmount,
              "deposit",
              pubKey.extractArray(),
              Hash32.ZERO.extractArray(),
              Bytes96.ZERO.extractArray());
      sb.createBlock();
      sb.createBlock();

      Assert.assertTrue(result.isSuccessful());
      Assert.assertEquals(1, result.getEvents().size());
    }

    Assert.assertFalse(chainStartMono.toFuture().isDone());

    sb.createBlock();
    sb.createBlock();
    sb.createBlock();

    ChainStart chainStart = chainStartMono.block(Duration.ofSeconds(1));

    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    Assert.assertEquals(
        1 + 16 + 31,
        sb.getBlockchain()
            .getBlockByHash(chainStart.getEth1Data().getBlockHash().extractArray())
            .getNumber());
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(
          (byte) i, chainStart.getInitialDeposits().get(i).getData().getPubKey().get(0));
    }

    Optional<Eth1Data> latestEth1Data1 = depositContract.getLatestEth1Data();
    Assert.assertTrue(latestEth1Data1.isPresent());
    Assert.assertEquals(chainStart.getEth1Data(), latestEth1Data1.get());

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        MutableBytes48 pubKey = MutableBytes48.create();
        pubKey.set(0, (byte) (0x20 + i * 4 + j));

        contract.callFunction(
            depositAmount,
            "deposit",
            pubKey.extractArray(),
            Hash32.ZERO.extractArray(),
            Bytes96.ZERO.extractArray());
      }
      sb.createBlock();
      sb.createBlock();
    }

    Optional<Eth1Data> latestEth1Data2 = depositContract.getLatestEth1Data();
    Assert.assertTrue(latestEth1Data2.isPresent());
    Assert.assertEquals(
        ethereum.getBlockchain().getBestBlock().getNumber() - 3,
        ethereum
            .getBlockchain()
            .getBlockByHash(latestEth1Data2.get().getBlockHash().extractArray())
            .getNumber());

    sb.createBlock();
    sb.createBlock();
    sb.createBlock();
    sb.createBlock();

    Optional<Eth1Data> latestEth1Data3 = depositContract.getLatestEth1Data();
    Assert.assertTrue(latestEth1Data3.isPresent());
    Assert.assertNotEquals(latestEth1Data2, latestEth1Data3);

    List<DepositInfo> allDepos = new ArrayList<>();
    Eth1Data from = chainStart.getEth1Data();
    while (true) {
      List<DepositInfo> infos = depositContract.peekDeposits(3, from, latestEth1Data3.get());
      if (infos.isEmpty()) break;
      allDepos.addAll(infos);
      from = infos.get(infos.size() - 1).getEth1Data();
    }
    Assert.assertEquals(16, allDepos.size());
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(0x20 + i, allDepos.get(i).getDeposit().getData().getPubKey().get(0));
    }
  }

  @Test
  public void testVerifyDepositRoot() throws InterruptedException {
    StandaloneBlockchain sb = new StandaloneBlockchain();
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractSource.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    sb.createBlock();

    Ethereum ethereum = new StandaloneEthereum(new SystemProperties(), sb);
    byte[] latestCalculatedDepositRoot = new byte[32];
    EthereumJDepositContract depositContract =
        new EthereumJDepositContract(
            ethereum,
            0,
            BytesValue.wrap(contract.getAddress()).toString(),
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH,
            createSpec()) {

          // avoid async block processing
          @Override
          protected void processConfirmedBlocks() {
            long bestConfirmedBlock = getBestConfirmedBlock();
            processBlocksUpTo(bestConfirmedBlock);
          }

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
    Mono<ChainStart> chainStartMono = Mono.from(depositContract.getChainStartMono());
    chainStartMono.subscribe();

    for (int i = 0; i < 20; i++) {
      MutableBytes48 pubKey = MutableBytes48.create();
      pubKey.set(0, (byte) i);

      SolidityCallResult result =
          contract.callFunction(
              depositAmount,
              "deposit",
              pubKey.extractArray(),
              Hash32.ZERO.extractArray(),
              Bytes96.ZERO.extractArray());
      sb.createBlock();
      sb.createBlock();
      sb.createBlock();
      sb.createBlock();

      Object[] depositRoot = contract.callConstFunction("get_hash_tree_root");
      Assert.assertArrayEquals((byte[]) depositRoot[0], latestCalculatedDepositRoot);

      Assert.assertTrue(result.isSuccessful());
      Assert.assertEquals(1, result.getEvents().size());
    }
  }

  @Test
  public void testVerifyProofs() {
    BeaconChainSpec specWithVerify =
        new BeaconChainSpec.Builder()
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
    BeaconChainSpec specWithoutVerify = createSpec();
    StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractSource.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    Object[] depositRoot = contract.callConstFunction("get_hash_tree_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    BlsKeyPairGenerator generator = BlsKeyPairGenerator.createWithoutSeed();
    List<Eth1Data> eth1DataList = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      BLS381.KeyPair keyPair = generator.next();
      Bytes48 pubKey = keyPair.getPublic().getEncodedBytes();
      Hash32 withdrawalCredentials =
          Hash32.wrap(Bytes32.leftPad(UInt64.valueOf(i).toBytesBigEndian()));
      DepositData depositData =
          new DepositData(
              BLSPubkey.wrap(pubKey),
              withdrawalCredentials,
              Gwei.castFrom(UInt64.valueOf(gweiAmount.longValue())),
              BLSSignature.ZERO);
      // Let signature be the result of bls_sign of the signing_root(deposit_data) with
      // domain=DOMAIN_DEPOSIT.
      MessageParameters messageParameters =
          MessageParameters.create(
              specWithVerify.signing_root(depositData), SignatureDomains.DEPOSIT);
      BLS381.Signature signature = BLS381.sign(messageParameters, keyPair);

      SolidityCallResult result =
          contract.callFunction(
              depositAmount,
              "deposit",
              pubKey.extractArray(),
              withdrawalCredentials.extractArray(),
              signature.getEncoded().extractArray());

      Assert.assertTrue(result.isSuccessful());

      depositRoot = contract.callConstFunction("get_hash_tree_root");
      Object[] depositCount = contract.callConstFunction("get_deposit_count");
      Eth1Data lastDepositEthData =
          new Eth1Data(
              Hash32.wrap(Bytes32.wrap((byte[]) depositRoot[0])),
              UInt64.fromBytesLittleEndian(Bytes8.wrap((byte[]) depositCount[0])),
              Hash32.ZERO);
      eth1DataList.add(lastDepositEthData);

      System.out.println(
          String.format(
              "root %d: %s",
              lastDepositEthData.getDepositCount().decrement().getValue(),
              lastDepositEthData.getDepositRoot()));

      Assert.assertEquals(1, result.getEvents().size());
    }

    for (int i = 0; i < 16; i++) {
      sb.createBlock();
    }

    Ethereum ethereum = new StandaloneEthereum(new SystemProperties(), sb);
    EthereumJDepositContract depositContract =
        new EthereumJDepositContract(
            ethereum,
            0,
            BytesValue.wrap(contract.getAddress()).toString(),
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH,
            specWithoutVerify);
    depositContract.setDistanceFromHead(3);

    ChainStart chainStart =
        Mono.from(depositContract.getChainStartMono()).block(Duration.ofSeconds(2));

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

    depositRoot = contract.callConstFunction("get_hash_tree_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    Eth1Data lastDepositEthData =
        new Eth1Data(
            Hash32.wrap(Bytes32.wrap((byte[]) depositRoot[0])),
            UInt64.ZERO,
            Hash32.wrap(Bytes32.wrap(sb.getBlockchain().getBlockByNumber(21).getHash())));

    List<DepositInfo> depositInfos1 =
        depositContract.peekDeposits(100, chainStart.getEth1Data(), lastDepositEthData);

    Assert.assertEquals(4, depositInfos1.size());
    for (DepositInfo depositInfo : depositInfos1) {
      beaconState.setEth1Data(
          eth1DataList.get(depositInfo.getEth1Data().getDepositCount().decrement().intValue()));
      specWithVerify.verify_deposit(beaconState, depositInfo.getDeposit());
      specWithVerify.process_deposit(beaconState, depositInfo.getDeposit());
    }
  }

  @Test
  public void testVyperAbi() {
    String abiTestBin =
        "0x6101db56600035601c52740100000000000000000000000000000000000000006020526f7fffffffffffffffffffffffffffffff6040527fffffffffffffffffffffffffffffffff8000000000000000000000000000000060605274012a05f1fffffffffffffffffffffffffdabf41c006080527ffffffffffffffffffffffffed5fa0e000000000000000000000000000000000060a05263d45754f860005114156101d157602060046101403760606004356004016101603760406004356004013511156100c957600080fd5b7f11111111111111111111111111111111111111111111111111111111111111116102405260406102005261020051610260526101608051602001806102005161024001828460006004600a8704601201f161012457600080fd5b505061020051610240015160206001820306601f820103905061020051610240016101e081516040818352015b836101e0511015156101625761017f565b60006101e0516020850101535b8151600101808352811415610151575b50505050602061020051610240015160206001820306601f8201039050610200510101610200527f68ab17451419beb01e059af9ee2a11c36d17b75ed25144e5cf78a0a469883ed161020051610240a1005b60006000fd5b6100046101db036100046000396100046101db036000f3";
    String abiTestAbi =
        "[{\"name\": \"Deposit\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"deposit_root\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"data\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"merkle_tree_index\", \"indexed\": false}, {\"type\": \"bytes32[32]\", \"name\": \"branch\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"ChainStart\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"deposit_root\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"time\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"Test\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"a\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"data\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"__init__\", \"outputs\": [], \"inputs\": [], \"constant\": false, \"payable\": false, \"type\": \"constructor\"}, {\"name\": \"get_deposit_root\", \"outputs\": [{\"type\": \"bytes32\", \"name\": \"out\"}], \"inputs\": [], \"constant\": true, \"payable\": false, \"type\": \"function\", \"gas\": 30775}, {\"name\": \"f\", \"outputs\": [], \"inputs\": [{\"type\": \"bytes\", \"name\": \"a\"}], \"constant\": false, \"payable\": true, \"type\": \"function\", \"gas\": 49719}, {\"name\": \"deposit\", \"outputs\": [], \"inputs\": [{\"type\": \"bytes\", \"name\": \"deposit_input\"}], \"constant\": false, \"payable\": true, \"type\": \"function\", \"gas\": 637708}]\n";
    StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = abiTestAbi.replaceAll(", *\"gas\": *[0-9]+", "");
    contractMetadata.bin = abiTestBin.replace("0x", "");
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    ((SolidityContractImpl) contract)
        .addRelatedContract(ContractSource.getContractAbi()); // TODO ethJ bug workaround

    byte[] bytes = new byte[64];
    Arrays.fill(bytes, (byte) 0x33);

    SolidityCallResult result = contract.callFunction("f", (Object) bytes);

    Object[] returnValues = result.getEvents().get(0).args;
    System.out.println(result);
  }
}
