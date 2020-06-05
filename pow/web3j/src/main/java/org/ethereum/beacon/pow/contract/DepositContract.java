package org.ethereum.beacon.pow.contract;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ethereum.beacon.pow.ContractSource;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.3.2-SNAPSHOT.
 *
 * <p>Generated with following input: org.web3j.codegen.SolidityFunctionWrapperGenerator.java -b /home/work/projects/beacon-chain-java/pow/core/src/main/resources/org/ethereum/beacon/pow/ContractBin.bin -a /home/work/projects/beacon-chain-java/pow/core/src/main/resources/org/ethereum/beacon/pow/ContractAbi.json -o /home/work/projects/beacon-chain-java/pow/web3j/src/main/java -p org.ethereum.beacon.pow.contract </p>
 */
public class DepositContract extends Contract {
    private static final String BINARY = ContractSource.getContractBin();

    public static final String FUNC_GET_HASH_TREE_ROOT = "get_hash_tree_root";

    public static final String FUNC_GET_DEPOSIT_COUNT = "get_deposit_count";

    public static final String FUNC_DEPOSIT = "deposit";

    public static final Event DEPOSITEVENT_EVENT = new Event("DepositEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<DynamicBytes>() {}, new TypeReference<DynamicBytes>() {}));
    ;

    @Deprecated
    protected DepositContract(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected DepositContract(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected DepositContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected DepositContract(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<DepositEventEventResponse> getDepositEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DEPOSITEVENT_EVENT, transactionReceipt);
        ArrayList<DepositEventEventResponse> responses = new ArrayList<DepositEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DepositEventEventResponse typedResponse = new DepositEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.pubkey = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.withdrawal_credentials = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.amount = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.signature = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
            typedResponse.index = (byte[]) eventValues.getNonIndexedValues().get(4).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DepositEventEventResponse> depositEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DepositEventEventResponse>() {
            @Override
            public DepositEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DEPOSITEVENT_EVENT, log);
                DepositEventEventResponse typedResponse = new DepositEventEventResponse();
                typedResponse.log = log;
                typedResponse.pubkey = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.withdrawal_credentials = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.amount = (byte[]) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.signature = (byte[]) eventValues.getNonIndexedValues().get(3).getValue();
                typedResponse.index = (byte[]) eventValues.getNonIndexedValues().get(4).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DepositEventEventResponse> depositEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEPOSITEVENT_EVENT));
        return depositEventEventFlowable(filter);
    }

    public RemoteCall<byte[]> get_hash_tree_root() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GET_HASH_TREE_ROOT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<byte[]> get_deposit_count() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GET_DEPOSIT_COUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicBytes>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteCall<TransactionReceipt> deposit(byte[] pubkey, byte[] withdrawal_credentials, byte[] signature, BigInteger weiValue) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_DEPOSIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(pubkey), 
                new org.web3j.abi.datatypes.DynamicBytes(withdrawal_credentials), 
                new org.web3j.abi.datatypes.DynamicBytes(signature)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    @Deprecated
    public static DepositContract load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new DepositContract(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static DepositContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new DepositContract(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static DepositContract load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new DepositContract(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static DepositContract load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new DepositContract(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<DepositContract> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DepositContract.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<DepositContract> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(DepositContract.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DepositContract> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DepositContract.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<DepositContract> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(DepositContract.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class DepositEventEventResponse {
        public Log log;

        public byte[] pubkey;

        public byte[] withdrawal_credentials;

        public byte[] amount;

        public byte[] signature;

        public byte[] index;
    }
}
