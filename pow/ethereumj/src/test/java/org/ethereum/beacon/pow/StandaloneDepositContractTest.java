package org.ethereum.beacon.pow;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.codec.binary.Hex;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.consensus.util.TransitionBeaconChainSpecSpec;
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
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.util.BlsKeyPairGenerator;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
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

@Ignore
public class StandaloneDepositContractTest {

  // modified contract:
  // CHAIN_START_FULL_DEPOSIT_THRESHOLD: constant(uint256) = 16  # 2**14
  // SECONDS_PER_DAY: constant(uint256) = 5

  final int MERKLE_TREE_DEPTH = SpecConstants.DEPOSIT_CONTRACT_TREE_DEPTH.intValue();
  final Function<BytesValue, Hash32> HASH_FUNCTION = Hashes::sha256;
  String depositBin =
      "600035601c52740100000000000000000000000000000000000000006020526f7fffffffffffffffffffffffffffffff6040527fffffffffffffffffffffffffffffffff8000000000000000000000000000000060605274012a05f1fffffffffffffffffffffffffdabf41c006080527ffffffffffffffffffffffffed5fa0e000000000000000000000000000000000060a052341561009e57600080fd5b6101406000601f818352015b600061014051602081106100bd57600080fd5b600060c052602060c020015460208261016001015260208101905061014051602081106100e957600080fd5b600060c052602060c020015460208261016001015260208101905080610160526101609050602060c0825160208401600060025af161012757600080fd5b60c0519050606051600161014051018060405190131561014657600080fd5b809190121561015457600080fd5b6020811061016157600080fd5b600060c052602060c0200155606051600161014051018060405190131561018757600080fd5b809190121561019557600080fd5b602081106101a257600080fd5b600060c052602060c020015460605160016101405101806040519013156101c857600080fd5b80919012156101d657600080fd5b602081106101e357600080fd5b600160c052602060c02001555b81516001018083528114156100aa575b50506115ea56600035601c52740100000000000000000000000000000000000000006020526f7fffffffffffffffffffffffffffffff6040527fffffffffffffffffffffffffffffffff8000000000000000000000000000000060605274012a05f1fffffffffffffffffffffffffdabf41c006080527ffffffffffffffffffffffffed5fa0e000000000000000000000000000000000060a0526380673289600051141561026b57602060046101403734156100b457600080fd5b67ffffffffffffffff6101405111156100cc57600080fd5b60006101605261014051610180526101a060006008818352015b6101605160086000811215610103578060000360020a820461010a565b8060020a82025b905090506101605260ff61018051166101c052610160516101c0516101605101101561013557600080fd5b6101c051610160510161016052610180517ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8600081121561017e578060000360020a8204610185565b8060020a82025b90509050610180525b81516001018083528114156100e6575b505060186008602082066101e001602082840111156101bc57600080fd5b60208061020082610160600060046015f15050818152809050905090508051602001806102a0828460006004600a8704601201f16101f957600080fd5b50506102a05160206001820306601f82010390506103006102a0516008818352015b8261030051111561022b57610247565b6000610300516102c001535b815160010180835281141561021b575b50505060206102805260406102a0510160206001820306601f8201039050610280f3005b639d70e8066000511415610405576020600461014037341561028c57600080fd5b60286004356004016101603760086004356004013511156102ac57600080fd5b60006101c0526101608060200151600082518060209013156102cd57600080fd5b80919012156102db57600080fd5b806020036101000a82049050905090506101e05261020060006008818352015b60ff6101e05116606051606051610200516007038060405190131561031f57600080fd5b809190121561032d57600080fd5b6008028060405190131561034057600080fd5b809190121561034e57600080fd5b6000811215610365578060000360020a820461036c565b8060020a82025b90509050610220526101c051610220516101c05101101561038c57600080fd5b610220516101c051016101c0526101e0517ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff860008112156103d5578060000360020a82046103dc565b8060020a82025b905090506101e0525b81516001018083528114156102fb575b50506101c05160005260206000f3005b63c5f2892f600051141561055d57341561041e57600080fd5b6000610140526002546101605261018060006020818352015b60016001610160511614156104b8576000610180516020811061045957600080fd5b600160c052602060c02001546020826102200101526020810190506101405160208261022001015260208101905080610220526102209050602060c0825160208401600060025af16104aa57600080fd5b60c051905061014052610526565b6000610140516020826101a001015260208101905061018051602081106104de57600080fd5b600060c052602060c02001546020826101a0010152602081019050806101a0526101a09050602060c0825160208401600060025af161051c57600080fd5b60c0519050610140525b610160600261053457600080fd5b60028151048152505b8151600101808352811415610437575b50506101405160005260206000f3005b63621fd130600051141561063357341561057657600080fd5b60606101c060246380673289610140526002546101605261015c6000305af161059e57600080fd5b6101e0805160200180610260828460006004600a8704601201f16105c157600080fd5b50506102605160206001820306601f82010390506102c0610260516008818352015b826102c05111156105f35761060f565b60006102c05161028001535b81516001018083528114156105e3575b5050506020610240526040610260510160206001820306601f8201039050610240f3005b63c47e300d60005114156113b757606060046101403760506004356004016101a037603060043560040135111561066957600080fd5b604060243560040161022037602060243560040135111561068957600080fd5b60806044356004016102803760606044356004013511156106a957600080fd5b633b9aca0061034052610340516106bf57600080fd5b61034051340461032052633b9aca006103205110156106dd57600080fd5b6060610440602463806732896103c052610320516103e0526103dc6000305af161070657600080fd5b610460805160200180610360828460006004600a8704601201f161072957600080fd5b50506002546104a05260006104c05260026104e05261050060006020818352015b60006104e05161075957600080fd5b6104e0516104a05160016104a05101101561077357600080fd5b60016104a0510106141515610787576107f3565b6104c06060516001825101806040519013156107a257600080fd5b80919012156107b057600080fd5b8152506104e0805115156107c55760006107df565b60028151600283510204146107d957600080fd5b60028151025b8152505b815160010180835281141561074a575b505060006101a06030806020846105e001018260208501600060046016f15050805182019150506000601060208206610560016020828401111561083657600080fd5b60208061058082610520600060046015f15050818152809050905090506010806020846105e001018260208501600060046013f1505080518201915050806105e0526105e09050602060c0825160208401600060025af161089657600080fd5b60c05190506105405260006000604060208206610680016102805182840111156108bf57600080fd5b6060806106a0826020602088068803016102800160006004601bf1505081815280905090509050602060c0825160208401600060025af16108ff57600080fd5b60c0519050602082610880010152602081019050600060406020602082066107400161028051828401111561093357600080fd5b606080610760826020602088068803016102800160006004601bf150508181528090509050905060208060208461080001018260208501600060046015f15050805182019150506105205160208261080001015260208101905080610800526108009050602060c0825160208401600060025af16109b057600080fd5b60c051905060208261088001015260208101905080610880526108809050602060c0825160208401600060025af16109e757600080fd5b60c051905061066052600060006105405160208261092001015260208101905061022060208060208461092001018260208501600060046015f150508051820191505080610920526109209050602060c0825160208401600060025af1610a4d57600080fd5b60c0519050602082610aa00101526020810190506000610360600880602084610a2001018260208501600060046012f150508051820191505060006018602082066109a00160208284011115610aa257600080fd5b6020806109c082610520600060046015f1505081815280905090509050601880602084610a2001018260208501600060046014f150508051820191505061066051602082610a2001015260208101905080610a2052610a209050602060c0825160208401600060025af1610b1557600080fd5b60c0519050602082610aa001015260208101905080610aa052610aa09050602060c0825160208401600060025af1610b4c57600080fd5b60c051905061090052610b2060006020818352015b6104c051610b20511215610be1576000610b205160208110610b8257600080fd5b600160c052602060c0200154602082610b4001015260208101905061090051602082610b4001015260208101905080610b4052610b409050602060c0825160208401600060025af1610bd357600080fd5b60c051905061090052610be6565b610bf7565b5b8151600101808352811415610b61575b5050610900516104c05160208110610c0e57600080fd5b600160c052602060c02001556002805460018254011015610c2e57600080fd5b60018154018155506020610c40600463c5f2892f610be052610bfc6000305af1610c5757600080fd5b610c4051610bc0526060610ce060246380673289610c60526104a051610c8052610c7c6000305af1610c8857600080fd5b610d00805160200180610d40828460006004600a8704601201f1610cab57600080fd5b505060a0610dc052610dc051610e00526101a0805160200180610dc051610e0001828460006004600a8704601201f1610ce357600080fd5b5050610dc051610e00015160206001820306601f8201039050610dc051610e0001610da081516040818352015b83610da051101515610d2157610d3e565b6000610da0516020850101535b8151600101808352811415610d10575b505050506020610dc051610e00015160206001820306601f8201039050610dc0510101610dc052610dc051610e2052610220805160200180610dc051610e0001828460006004600a8704601201f1610d9557600080fd5b5050610dc051610e00015160206001820306601f8201039050610dc051610e0001610da081516020818352015b83610da051101515610dd357610df0565b6000610da0516020850101535b8151600101808352811415610dc2575b505050506020610dc051610e00015160206001820306601f8201039050610dc0510101610dc052610dc051610e4052610360805160200180610dc051610e0001828460006004600a8704601201f1610e4757600080fd5b5050610dc051610e00015160206001820306601f8201039050610dc051610e0001610da081516020818352015b83610da051101515610e8557610ea2565b6000610da0516020850101535b8151600101808352811415610e74575b505050506020610dc051610e00015160206001820306601f8201039050610dc0510101610dc052610dc051610e6052610280805160200180610dc051610e0001828460006004600a8704601201f1610ef957600080fd5b5050610dc051610e00015160206001820306601f8201039050610dc051610e0001610da081516060818352015b83610da051101515610f3757610f54565b6000610da0516020850101535b8151600101808352811415610f26575b505050506020610dc051610e00015160206001820306601f8201039050610dc0510101610dc052610dc051610e8052610d40805160200180610dc051610e0001828460006004600a8704601201f1610fab57600080fd5b5050610dc051610e00015160206001820306601f8201039050610dc051610e0001610da081516020818352015b83610da051101515610fe957611006565b6000610da0516020850101535b8151600101808352811415610fd8575b505050506020610dc051610e00015160206001820306601f8201039050610dc0510101610dc0527fdc5fc95703516abd38fa03c3737ff3b52dc52347055c8028460fdf5bbe2f12ce610dc051610e00a1640773594000610320511015156113b557600380546001825401101561107b57600080fd5b6001815401815550601060035414156113b45742610ec05242610ee05260056110a357600080fd5b6005610ee05106610ec05110156110b957600080fd5b42610ee05260056110c957600080fd5b6005610ee05106610ec05103600a42610ec05242610ee05260056110ec57600080fd5b6005610ee05106610ec051101561110257600080fd5b42610ee052600561111257600080fd5b6005610ee05106610ec0510301101561112a57600080fd5b600a42610ec05242610ee052600561114157600080fd5b6005610ee05106610ec051101561115757600080fd5b42610ee052600561116757600080fd5b6005610ee05106610ec0510301610ea0526060610f8060246380673289610f0052600254610f2052610f1c6000305af16111a057600080fd5b610fa0805160200180610fe0828460006004600a8704601201f16111c357600080fd5b505060606110c06024638067328961104052610ea0516110605261105c6000305af16111ee57600080fd5b6110e0805160200180611120828460006004600a8704601201f161121157600080fd5b5050610bc0516111e05260606111a0526111a05161120052610fe08051602001806111a0516111e001828460006004600a8704601201f161125157600080fd5b50506111a0516111e0015160206001820306601f82010390506111a0516111e00161118081516020818352015b836111805110151561128f576112ac565b6000611180516020850101535b815160010180835281141561127e575b5050505060206111a0516111e0015160206001820306601f82010390506111a05101016111a0526111a051611220526111208051602001806111a0516111e001828460006004600a8704601201f161130357600080fd5b50506111a0516111e0015160206001820306601f82010390506111a0516111e00161118081516020818352015b83611180511015156113415761135e565b6000611180516020850101535b8151600101808352811415611330575b5050505060206111a0516111e0015160206001820306601f82010390506111a05101016111a0527f08b71ef3f1b58f7a23ffb82e27f12f0888c8403f1ceb0ea7ea26b274e2189d4c6111a0516111e0a160016004555b5b005b63845980e860005114156113dd5734156113d057600080fd5b60045460005260206000f3005b60006000fd5b6102076115ea036102076000396102076115ea036000f3";
  String abiTestBin =
      "0x6101db56600035601c52740100000000000000000000000000000000000000006020526f7fffffffffffffffffffffffffffffff6040527fffffffffffffffffffffffffffffffff8000000000000000000000000000000060605274012a05f1fffffffffffffffffffffffffdabf41c006080527ffffffffffffffffffffffffed5fa0e000000000000000000000000000000000060a05263d45754f860005114156101d157602060046101403760606004356004016101603760406004356004013511156100c957600080fd5b7f11111111111111111111111111111111111111111111111111111111111111116102405260406102005261020051610260526101608051602001806102005161024001828460006004600a8704601201f161012457600080fd5b505061020051610240015160206001820306601f820103905061020051610240016101e081516040818352015b836101e0511015156101625761017f565b60006101e0516020850101535b8151600101808352811415610151575b50505050602061020051610240015160206001820306601f8201039050610200510101610200527f68ab17451419beb01e059af9ee2a11c36d17b75ed25144e5cf78a0a469883ed161020051610240a1005b60006000fd5b6100046101db036100046000396100046101db036000f3";
  String abiTestAbi =
      "[{\"name\": \"Deposit\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"deposit_root\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"data\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"merkle_tree_index\", \"indexed\": false}, {\"type\": \"bytes32[32]\", \"name\": \"branch\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"ChainStart\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"deposit_root\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"time\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"Test\", \"inputs\": [{\"type\": \"bytes32\", \"name\": \"a\", \"indexed\": false}, {\"type\": \"bytes\", \"name\": \"data\", \"indexed\": false}], \"anonymous\": false, \"type\": \"event\"}, {\"name\": \"__init__\", \"outputs\": [], \"inputs\": [], \"constant\": false, \"payable\": false, \"type\": \"constructor\"}, {\"name\": \"get_deposit_root\", \"outputs\": [{\"type\": \"bytes32\", \"name\": \"out\"}], \"inputs\": [], \"constant\": true, \"payable\": false, \"type\": \"function\", \"gas\": 30775}, {\"name\": \"f\", \"outputs\": [], \"inputs\": [{\"type\": \"bytes\", \"name\": \"a\"}], \"constant\": false, \"payable\": true, \"type\": \"function\", \"gas\": 49719}, {\"name\": \"deposit\", \"outputs\": [], \"inputs\": [{\"type\": \"bytes\", \"name\": \"deposit_input\"}], \"constant\": false, \"payable\": true, \"type\": \"function\", \"gas\": 637708}]\n";
  BigInteger gweiAmount = BigInteger.valueOf(32L * 1_000_000_000L);
  BigInteger depositAmount = gweiAmount.multiply(BigInteger.valueOf(1_000_000_000L));

  @Test
  public void test1() {
    StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractAbi.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    Object[] depositRoot = contract.callConstFunction("get_deposit_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    SSZSerializer sszSerializer = new SSZBuilder().buildSerializer();

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
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH);
    depositContract.setDistanceFromHead(3);

    ChainStart chainStart =
        Mono.from(depositContract.getChainStartMono()).block(Duration.ofSeconds(2));

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

    depositRoot = contract.callConstFunction("get_deposit_root");
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
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH);
    depositContract.setDistanceFromHead(3);
    Mono<ChainStart> chainStartMono = Mono.from(depositContract.getChainStartMono());
    chainStartMono.subscribe();

    SSZSerializer sszSerializer = new SSZBuilder().buildSerializer();

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
      Assert.assertEquals(i == 15 ? 2 : 1, result.getEvents().size());
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
  public void testVerifyDepositRoot() {
    StandaloneBlockchain sb = new StandaloneBlockchain();
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractAbi.getContractAbi();
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
            MERKLE_TREE_DEPTH) {

          @Override
          protected synchronized void newDeposits(
              List<DepositEventData> eventDataList, byte[] blockHash) {
            super.newDeposits(eventDataList, blockHash);
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

      Object[] depositRoot = contract.callConstFunction("get_deposit_root");
      Assert.assertArrayEquals((byte[]) depositRoot[0], latestCalculatedDepositRoot);

      Assert.assertTrue(result.isSuccessful());
      Assert.assertEquals(i == 15 ? 2 : 1, result.getEvents().size());
    }
  }

  @Test
  public void testVerifyProofs() {
    StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = ContractAbi.getContractAbi();
    contractMetadata.bin = depositBin;
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    Object[] depositRoot = contract.callConstFunction("get_deposit_root");
    System.out.println(Hex.encodeHexString((byte[]) depositRoot[0]));

    BlsKeyPairGenerator generator = BlsKeyPairGenerator.createWithoutSeed();
    BeaconChainSpec spec =
        new TransitionBeaconChainSpecSpec(
            BeaconChainSpec.DEFAULT_CONSTANTS,
            Hashes::sha256,
            ObjectHasher.createSSZOverSHA256(BeaconChainSpec.DEFAULT_CONSTANTS),
            true,
            false);
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
          MessageParameters.create(spec.signing_root(depositData), SignatureDomains.DEPOSIT);
      BLS381.Signature signature = BLS381.sign(messageParameters, keyPair);

      SolidityCallResult result =
          contract.callFunction(
              depositAmount,
              "deposit",
              pubKey.extractArray(),
              withdrawalCredentials.extractArray(),
              signature.getEncoded().extractArray());

      Assert.assertTrue(result.isSuccessful());

      depositRoot = contract.callConstFunction("get_deposit_root");
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
            Schedulers.createDefault(),
            HASH_FUNCTION,
            MERKLE_TREE_DEPTH);
    depositContract.setDistanceFromHead(3);

    ChainStart chainStart =
        Mono.from(depositContract.getChainStartMono()).block(Duration.ofSeconds(2));

    Assert.assertEquals(16, chainStart.getInitialDeposits().size());
    MutableBeaconState beaconState = BeaconState.getEmpty().createMutableCopy();
    int i = 0;
    for (Deposit deposit : chainStart.getInitialDeposits()) {
      //       The proof for each deposit must be constructed against the deposit root contained in
      // state.latest_eth1_data rather than the deposit root at the time the deposit was initially
      // logged from the 1.0 chain. This entails storing a full deposit merkle tree locally and
      // computing updated proofs against the latest_eth1_data.deposit_root as needed. See
      // minimal_merkle.py for a sample implementation.
      beaconState.setLatestEth1Data(eth1DataList.get(i));
      spec.verify_deposit(beaconState, deposit);
      spec.process_deposit(beaconState, deposit);
      i += 1;
    }

    depositRoot = contract.callConstFunction("get_deposit_root");
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
      beaconState.setLatestEth1Data(
          eth1DataList.get(depositInfo.getEth1Data().getDepositCount().decrement().intValue()));
      spec.verify_deposit(beaconState, depositInfo.getDeposit());
      spec.process_deposit(beaconState, depositInfo.getDeposit());
    }
  }

  @Test
  public void testVyperAbi() {
    StandaloneBlockchain sb = new StandaloneBlockchain().withAutoblock(true);
    ContractMetadata contractMetadata = new ContractMetadata();
    contractMetadata.abi = abiTestAbi.replaceAll(", *\"gas\": *[0-9]+", "");
    contractMetadata.bin = abiTestBin.replace("0x", "");
    SolidityContract contract = sb.submitNewContract(contractMetadata);
    ((SolidityContractImpl) contract)
        .addRelatedContract(ContractAbi.getContractAbi()); // TODO ethJ bug workaround

    byte[] bytes = new byte[64];
    Arrays.fill(bytes, (byte) 0x33);

    SolidityCallResult result = contract.callFunction("f", (Object) bytes);

    Object[] returnValues = result.getEvents().get(0).args;
    System.out.println(result);
  }
}
