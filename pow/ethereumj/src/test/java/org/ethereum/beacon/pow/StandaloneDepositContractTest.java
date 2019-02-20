package org.ethereum.beacon.pow;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.schedulers.DefaultSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.Serializer;
import org.ethereum.config.SystemProperties;
import org.ethereum.facade.Ethereum;
import org.ethereum.solidity.compiler.CompilationResult.ContractMetadata;
import org.ethereum.util.blockchain.SolidityCallResult;
import org.ethereum.util.blockchain.SolidityContract;
import org.ethereum.util.blockchain.StandaloneBlockchain;
import org.ethereum.util.blockchain.StandaloneBlockchain.SolidityContractImpl;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytes48;
import tech.pegasys.artemis.util.uint.UInt64;

public class StandaloneDepositContractTest {

  // modified contract:
  // CHAIN_START_FULL_DEPOSIT_THRESHOLD: constant(uint256) = 16  # 2**14
  // SECONDS_PER_DAY: constant(uint256) = 5

  String depositBin = "600035601c52740100000000000000000000000000000000000000006020526f7fffffffffffffffffffffffffffffff6040527fffffffffffffffffffffffffffffffff8000000000000000000000000000000060605274012a05f1fffffffffffffffffffffffffdabf41c006080527ffffffffffffffffffffffffed5fa0e000000000000000000000000000000000060a052341561009e57600080fd5b6101406000601f818352015b600061014051602081106100bd57600080fd5b600060c052602060c020015460208261016001015260208101905061014051602081106100e957600080fd5b600060c052602060c020015460208261016001015260208101905080610160526101609050805160208201209050606051600161014051018060405190131561013157600080fd5b809190121561013f57600080fd5b6020811061014c57600080fd5b600060c052602060c0200155606051600161014051018060405190131561017257600080fd5b809190121561018057600080fd5b6020811061018d57600080fd5b600060c052602060c020015460605160016101405101806040519013156101b357600080fd5b80919012156101c157600080fd5b602081106101ce57600080fd5b600160c052602060c02001555b81516001018083528114156100aa575b5050610f2f56600035601c52740100000000000000000000000000000000000000006020526f7fffffffffffffffffffffffffffffff6040527fffffffffffffffffffffffffffffffff8000000000000000000000000000000060605274012a05f1fffffffffffffffffffffffffdabf41c006080527ffffffffffffffffffffffffed5fa0e000000000000000000000000000000000060a05263c5f2892f60005114156101cc5734156100ac57600080fd5b6000610140526002546101605261018060006020818352015b600160026100d257600080fd5b60026101605106141561013c57600061018051602081106100f257600080fd5b600160c052602060c0200154602082610220010152602081019050610140516020826102200101526020810190508061022052610220905080516020820120905061014052610195565b6000610140516020826101a0010152602081019050610180516020811061016257600080fd5b600060c052602060c02001546020826101a0010152602081019050806101a0526101a09050805160208201209050610140525b61016060026101a357600080fd5b60028151048152505b81516001018083528114156100c5575b50506101405160005260206000f3005b6398b1e06a6000511415610d375760206004610140376108206004356004016101603761080060043560040135111561020457600080fd5b670de0b6b3a764000034101561021957600080fd5b6801bc16d674ec80000034111561022f57600080fd5b6002546109a0526018600860208206610ac0016000633b9aca0061025257600080fd5b633b9aca003404602082610a6001015260208101905080610a6052610a60905051828401111561028157600080fd5b602080610ae0826020602088068803016000633b9aca006102a157600080fd5b633b9aca003404602082610a6001015260208101905080610a6052610a60905001600060046015f15050818152809050905090508051602001806109c0828460006004600a8704601201f16102f557600080fd5b50506018600860208206610c4001600042602082610be001015260208101905080610be052610be0905051828401111561032e57600080fd5b602080610c6082602060208806880301600042602082610be001015260208101905080610be052610be0905001600060046015f1505081815280905090509050805160200180610b40828460006004600a8704601201f161038e57600080fd5b505060006109c060088060208461152001018260208501600060046012f1505080518201915050610b4060088060208461152001018260208501600060046012f150508051820191505061016061080080602084611520010182602085016000600460def150508051820191505080611520526115209050805160200180610cc0828460006004600a8704601201f161042657600080fd5b50506018600860208206611e800160006109a051602082611e2001015260208101905080611e2052611e20905051828401111561046257600080fd5b602080611ea08260206020880688030160006109a051602082611e2001015260208101905080611e2052611e20905001600060046015f1505081815280905090509050805160200180611d80828460006004600a8704601201f16104c557600080fd5b50506000611f00526002611f2052611f4060006020818352015b6000611f20516104ee57600080fd5b611f20516109a05160016109a05101101561050857600080fd5b60016109a051010614151561051c57610588565b611f0060605160018251018060405190131561053757600080fd5b809190121561054557600080fd5b815250611f208051151561055a576000610574565b600281516002835102041461056e57600080fd5b60028151025b8152505b81516001018083528114156104df575b5050610cc0805160208201209050611f6052611f8060006020818352015b611f0051611f8051121561060d576000611f8051602081106105c757600080fd5b600160c052602060c0200154602082611fa0010152602081019050611f6051602082611fa001015260208101905080611fa052611fa09050805160208201209050611f60525b5b81516001018083528114156105a6575b5050611f6051611f00516020811061063557600080fd5b600160c052602060c0200155600280546001825401101561065557600080fd5b60018154018155506020612080600463c5f2892f6120205261203c6000305af161067e57600080fd5b612080516120a0526120a05161212052600160c052602060c02054612180526001600160c052602060c02001546121a0526002600160c052602060c02001546121c0526003600160c052602060c02001546121e0526004600160c052602060c0200154612200526005600160c052602060c0200154612220526006600160c052602060c0200154612240526007600160c052602060c0200154612260526008600160c052602060c0200154612280526009600160c052602060c02001546122a052600a600160c052602060c02001546122c052600b600160c052602060c02001546122e052600c600160c052602060c020015461230052600d600160c052602060c020015461232052600e600160c052602060c020015461234052600f600160c052602060c0200154612360526010600160c052602060c0200154612380526011600160c052602060c02001546123a0526012600160c052602060c02001546123c0526013600160c052602060c02001546123e0526014600160c052602060c0200154612400526015600160c052602060c0200154612420526016600160c052602060c0200154612440526017600160c052602060c0200154612460526018600160c052602060c0200154612480526019600160c052602060c02001546124a052601a600160c052602060c02001546124c052601b600160c052602060c02001546124e052601c600160c052602060c020015461250052601d600160c052602060c020015461252052601e600160c052602060c020015461254052601f600160c052602060c0200154612560526104606120e0526120e05161214052610cc08051602001806120e05161212001828460006004600a8704601201f161090257600080fd5b50506120e051612120015160206001820306601f82010390506120e051612120016120c08151610820818352015b836120c0511015156109415761095e565b60006120c0516020850101535b8151600101808352811415610930575b5050505060206120e051612120015160206001820306601f82010390506120e05101016120e0526120e05161216052611d808051602001806120e05161212001828460006004600a8704601201f16109b557600080fd5b50506120e051612120015160206001820306601f82010390506120e051612120016120c081516020818352015b836120c0511015156109f357610a10565b60006120c0516020850101535b81516001018083528114156109e2575b5050505060206120e051612120015160206001820306601f82010390506120e05101016120e0527fce7a77a358682d6c81f71216fb7fb108b03bc8badbf67f5d131ba5363cbefb426120e051612120a16801bc16d674ec800000341415610d35576003805460018254011015610a8557600080fd5b600181540181555060106003541415610d3457426125a052426125c0526005610aad57600080fd5b60056125c051066125a0511015610ac357600080fd5b426125c0526005610ad357600080fd5b60056125c051066125a051036005426125a052426125c0526005610af657600080fd5b60056125c051066125a0511015610b0c57600080fd5b426125c0526005610b1c57600080fd5b60056125c051066125a05103011015610b3457600080fd5b6005426125a052426125c0526005610b4b57600080fd5b60056125c051066125a0511015610b6157600080fd5b426125c0526005610b7157600080fd5b60056125c051066125a05103016125805260186008602082066126e00160006125805160208261268001015260208101905080612680526126809050518284011115610bbc57600080fd5b602080612700826020602088068803016000612580516020826126800101526020810190508061268052612680905001600060046015f15050818152809050905090508051602001806125e0828460006004600a8704601201f1610c1f57600080fd5b505060206127c0600463c5f2892f6127605261277c6000305af1610c4257600080fd5b6127c0516127e0526127e0516128605260406128205261282051612880526125e08051602001806128205161286001828460006004600a8704601201f1610c8857600080fd5b505061282051612860015160206001820306601f8201039050612820516128600161280081516020818352015b8361280051101515610cc657610ce3565b6000612800516020850101535b8151600101808352811415610cb5575b50505050602061282051612860015160206001820306601f8201039050612820510101612820527fd1faa3f9bca1d698df559716fe6d1c9999155b38d3158fffbc98d76d568091fc61282051612860a15b5b005b60006000fd5b6101f2610f2f036101f26000396101f2610f2f036000f3";
  String abiTestBin = "0x6101db56600035601c52740100000000000000000000000000000000000000006020526f7fffffffffffffffffffffffffffffff6040527fffffffffffffffffffffffffffffffff8000000000000000000000000000000060605274012a05f1fffffffffffffffffffffffffdabf41c006080527ffffffffffffffffffffffffed5fa0e000000000000000000000000000000000060a05263d45754f860005114156101d157602060046101403760606004356004016101603760406004356004013511156100c957600080fd5b7f11111111111111111111111111111111111111111111111111111111111111116102405260406102005261020051610260526101608051602001806102005161024001828460006004600a8704601201f161012457600080fd5b505061020051610240015160206001820306601f820103905061020051610240016101e081516040818352015b836101e0511015156101625761017f565b60006101e0516020850101535b8151600101808352811415610151575b50505050602061020051610240015160206001820306601f8201039050610200510101610200527f68ab17451419beb01e059af9ee2a11c36d17b75ed25144e5cf78a0a469883ed161020051610240a1005b60006000fd5b6100046101db036100046000396100046101db036000f3";

  String abiTestAbi =
      "[{\"name\": \"Deposit\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"deposit_root\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"data\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"merkle_tree_index\", \"indexed\": false}, {\"type\": \"bytes32[32]\", \"name\": \"branch\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"ChainStart\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"deposit_root\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"time\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"Test\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"a\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"data\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"__init__\", \"outputs\": [], \"inputs\": [], \"constant\": false, \"payable\": false, \"type\": \"constructor\"}, {\"name\": \"get_deposit_root\", \"outputs\": [{\"type\": \"bytes32\", \"name\": \"out\"}], \"inputs\": [], \"constant\": true, \"payable\": false, \"type\": \"function\", \"gas\": 30775}, {\"name\": \"f\", \"outputs\": [], \"inputs\": [{\"type\": \"bytes\", \"name\": \"a\"}], \"constant\": false, \"payable\": true, \"type\": \"function\", \"gas\": 49719}, {\"name\": \"deposit\", \"outputs\": [], \"inputs\": [{\"type\": \"bytes\", \"name\": \"deposit_input\"}], \"constant\": false, \"payable\": true, \"type\": \"function\", \"gas\": 637708}]\n";

  BigInteger depositAmount =
      BigInteger.valueOf(32L * 1_000_000_000L).multiply(BigInteger.valueOf(1_000_000_000L));

  @Test
  public void test1() {
    StandaloneBlockchain sb = new StandaloneBlockchain()
        .withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractAbi.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    Object[] depositRoot = contract.callConstFunction("get_deposit_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    Serializer sszSerializer = Serializer.annotationSerializer();

    for(int i = 0; i < 20; i++) {
      MutableBytes48 pubKey = MutableBytes48.create();
      pubKey.set(0, (byte) i);
      DepositInput depositInput = new DepositInput(BLSPubkey.wrap(pubKey), Hash32.ZERO, BLSSignature.wrap(Bytes96.ZERO));
      BytesValue depositInputBytes = sszSerializer.encode2(depositInput);

      SolidityCallResult result = contract.callFunction(
          depositAmount,
          "deposit",
          (Object) depositInputBytes.extractArray());

      Assert.assertTrue(result.isSuccessful());
      Assert.assertEquals(i == 15 ? 2 : 1, result.getEvents().size());
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
            Schedulers.createDefault());
    depositContract.setDistanceFromHead(3);

    ChainStart chainStart = Mono.from(depositContract.getChainStartMono())
        .block(Duration.ofSeconds(2));

    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    Assert.assertEquals(17, sb.getBlockchain()
        .getBlockByHash(chainStart.getEth1Data().getBlockHash().extractArray()).getNumber());
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(UInt64.valueOf(i), chainStart.getInitialDeposits().get(i).getIndex());
      Assert.assertEquals((byte) i, chainStart.getInitialDeposits().get(i).getDepositData()
          .getDepositInput().getPubKey().get(0));
    }

    depositRoot = contract.callConstFunction("get_deposit_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    Eth1Data lastDepositEthData = new Eth1Data(
        Hash32.wrap(Bytes32.wrap((byte[]) depositRoot[0])),
        Hash32.wrap(Bytes32.wrap(sb.getBlockchain().getBlockByNumber(21).getHash())));

    List<DepositInfo> depositInfos1 = depositContract.peekDeposits(2,
        chainStart.getEth1Data(), lastDepositEthData);

    Assert.assertEquals(2, depositInfos1.size());
    Assert.assertEquals((byte) 16,
        depositInfos1.get(0).getDeposit().getDepositData().getDepositInput().getPubKey().get(0));
    Assert.assertEquals((byte) 17,
        depositInfos1.get(1).getDeposit().getDepositData().getDepositInput().getPubKey().get(0));

    List<DepositInfo> depositInfos2 = depositContract.peekDeposits(200,
        depositInfos1.get(1).getEth1Data(), lastDepositEthData);

    Assert.assertEquals(2, depositInfos2.size());
    Assert.assertEquals((byte) 18,
        depositInfos2.get(0).getDeposit().getDepositData().getDepositInput().getPubKey().get(0));
    Assert.assertEquals((byte) 19,
        depositInfos2.get(1).getDeposit().getDepositData().getDepositInput().getPubKey().get(0));

    List<DepositInfo> depositInfos3 = depositContract.peekDeposits(200,
        lastDepositEthData, lastDepositEthData);
    Assert.assertEquals(0, depositInfos3.size());
  }

  @Test
  public void testOnline() {
    StandaloneBlockchain sb = new StandaloneBlockchain();
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractAbi.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    sb.createBlock();

    Ethereum ethereum = new StandaloneEthereum(new SystemProperties(), sb);
    EthereumJDepositContract depositContract =
        new EthereumJDepositContract(
            ethereum,
            0,
            BytesValue.wrap(contract.getAddress()).toString(),
            Schedulers.createDefault());
    depositContract.setDistanceFromHead(3);
    Mono<ChainStart> chainStartMono = Mono.from(depositContract.getChainStartMono());
    chainStartMono.subscribe();

    Serializer sszSerializer = Serializer.annotationSerializer();

    for(int i = 0; i < 16; i++) {
      sb.createBlock();
    }

    for(int i = 0; i < 16; i++) {
      MutableBytes48 pubKey = MutableBytes48.create();
      pubKey.set(0, (byte) i);
      DepositInput depositInput = new DepositInput(BLSPubkey.wrap(pubKey), Hash32.ZERO, BLSSignature.wrap(Bytes96.ZERO));
      BytesValue depositInputBytes = sszSerializer.encode2(depositInput);

      SolidityCallResult result = contract.callFunction(
          depositAmount,
          "deposit",
          (Object) depositInputBytes.extractArray());
      sb.createBlock();
      sb.createBlock();

      Assert.assertTrue(result.isSuccessful());
      Assert.assertEquals(i == 15 ? 2 : 1, result.getEvents().size());
    }

    Assert.assertFalse(chainStartMono.toFuture().isDone());

    sb.createBlock();
    sb.createBlock();
    sb.createBlock();

    ChainStart chainStart = chainStartMono.block(Duration.ofSeconds(1));

    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    Assert.assertEquals(1 + 16 + 31, sb.getBlockchain()
        .getBlockByHash(chainStart.getEth1Data().getBlockHash().extractArray()).getNumber());
    for (int i = 0; i < 16; i++) {
      Assert.assertEquals(UInt64.valueOf(i), chainStart.getInitialDeposits().get(i).getIndex());
      Assert.assertEquals((byte) i, chainStart.getInitialDeposits().get(i).getDepositData()
          .getDepositInput().getPubKey().get(0));
    }

    Optional<Eth1Data> latestEth1Data1 = depositContract.getLatestEth1Data();
    Assert.assertTrue(latestEth1Data1.isPresent());
    Assert.assertEquals(chainStart.getEth1Data(), latestEth1Data1.get());

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        MutableBytes48 pubKey = MutableBytes48.create();
        pubKey.set(0, (byte) (0x20 + i * 4 + j));
        DepositInput depositInput = new DepositInput(BLSPubkey.wrap(pubKey), Hash32.ZERO, BLSSignature.wrap(Bytes96.ZERO));
        BytesValue depositInputBytes = sszSerializer.encode2(depositInput);

        contract.callFunction(depositAmount,"deposit",
            (Object) depositInputBytes.extractArray());
      }
      sb.createBlock();
      sb.createBlock();
    }

    Optional<Eth1Data> latestEth1Data2 = depositContract.getLatestEth1Data();
    Assert.assertTrue(latestEth1Data2.isPresent());
    Assert.assertEquals(ethereum.getBlockchain().getBestBlock().getNumber() - 3,
        ethereum.getBlockchain().getBlockByHash(latestEth1Data2.get().getBlockHash().extractArray()).getNumber());

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
      Assert.assertEquals(0x20 + i, allDepos.get(i).getDeposit().getDepositData()
          .getDepositInput().getPubKey().get(0));
    }
  }

  @Test
  public void testVyperAbi() {
    StandaloneBlockchain sb = new StandaloneBlockchain()
        .withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = abiTestAbi.replaceAll(", *\"gas\": *[0-9]+", "");
    contractMetadata.bin = abiTestBin.replace("0x", "");
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    ((SolidityContractImpl) contract).addRelatedContract(ContractAbi.getContractAbi()); // TODO ethJ bug workaround

      byte[] bytes = new byte[64];
      Arrays.fill(bytes, (byte) 0x33);

      SolidityCallResult result = contract.callFunction(
          "f",
          (Object) bytes);

    Object[] returnValues = result.getEvents().get(0).args;
    System.out.println(result);
  }
}
