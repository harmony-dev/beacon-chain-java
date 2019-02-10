package org.ethereum.beacon.pow.validator;

import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.ssz.Hasher;
import org.ethereum.beacon.ssz.SSZHasher;
import org.ethereum.beacon.ssz.Serializer;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import static org.ethereum.beacon.core.spec.SignatureDomains.DEPOSIT;

public class ValidatorRegistrationService {
  TransactionBuilder transactionBuilder;
  TransactionGateway transactionGateway;
  DepositContract depositContract;
  SpecHelpers specHelpers;
  Hasher<Hash32> sszHasher;
  Serializer sszSerializer;
  BeaconState latestState;
  Publisher<ObservableBeaconState> observablePublisher;

  // Validator
  MessageSigner<BLSSignature> signer;
  BLSPubkey pubKey;
  Hash32 withdrawalCredentials;

  private ScheduledExecutorService executor;

  public ValidatorRegistrationService(
      TransactionBuilder transactionBuilder,
      TransactionGateway transactionGateway,
      Publisher<ObservableBeaconState> observablePublisher,
      SpecHelpers specHelpers) {
    this.transactionBuilder = transactionBuilder;
    this.transactionGateway = transactionGateway;
    this.observablePublisher = observablePublisher;
    this.specHelpers = specHelpers;
    Function<BytesValue, BytesValue> hashingFunction =
        (data) ->
            BytesValue.wrap(
                Hashes.keccak256(BytesValue.of(data.getArrayUnsafe())).getArrayUnsafe());
    sszHasher = SSZHasher.simpleHasher(hashingFunction);
    sszSerializer = Serializer.annotationSerializer();
  }

  public void start() {
    // TODO: use me
    this.executor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread t = new Thread(runnable, "validator-registration-service");
              t.setDaemon(true);
              return t;
            });
    Flux.from(observablePublisher).subscribe(o -> latestState = o.getLatestSlotState());
    // TODO: issue 1: if we have only amount we couldn't be sure it was this exact deposit
    // easy solution: only full deposit for beginning
    // TODO: issue 2: where to get old DepositData? We either should keep some state in local db/or
    // use state to check all validators for inclusion

    // TODO: understand current stage

    // TODO: run next stage
  }

  public CompletableFuture<TransactionGateway.TxStatus> submitDeposit(
      Gwei amount, Address eth1From, BytesValue eth1PrivKey) {
    DepositInput depositInput = createDepositInput();
    BytesValue tx = createTransaction(eth1From, eth1PrivKey, depositInput, amount);
    return transactionGateway.send(tx);
  }

  private DepositInput createDepositInput() {
    // To submit a deposit:
    //
    //    Pack the validator's initialization parameters into deposit_input, a DepositInput SSZ
    // object.
    //    Set deposit_input.proof_of_possession = EMPTY_SIGNATURE.
    DepositInput preDepositInput =
        new DepositInput(pubKey, withdrawalCredentials, BLSSignature.ZERO);
    // Let proof_of_possession be the result of bls_sign of the hash_tree_root(deposit_input) with
    // domain=DOMAIN_DEPOSIT.
    Hash32 hash = sszHasher.calc(preDepositInput);
    Bytes8 domain =
        specHelpers.get_domain(
            latestState.getForkData(), specHelpers.get_current_epoch(latestState), DEPOSIT);
    BLSSignature signature = signer.sign(hash, domain);
    // Set deposit_input.proof_of_possession = proof_of_possession.
    DepositInput depositInput = new DepositInput(pubKey, withdrawalCredentials, signature);

    return depositInput;
  }

  public BytesValue createTransaction(
      Address eth1From, BytesValue eth1PrivKey, DepositInput depositInput, Gwei amount) {
    // Let amount be the amount in Gwei to be deposited by the validator where MIN_DEPOSIT_AMOUNT <=
    // amount <= MAX_DEPOSIT_AMOUNT.
    // TODO: shouldn't be asserts
    assert amount.compareTo(specHelpers.getChainSpec().getMinDepositAmount()) >= 0;
    assert amount.compareTo(specHelpers.getChainSpec().getMaxDepositAmount()) <= 0;
    // Send a transaction on the Ethereum 1.0 chain to DEPOSIT_CONTRACT_ADDRESS executing deposit
    // along with serialize(deposit_input) as the singular bytes input along with a deposit amount
    // in Gwei.
    BytesValue unsignedTx =
        transactionBuilder.createTransaction(
            eth1From.toString(), sszSerializer.encode2(depositInput), amount);

    return transactionBuilder.signTransaction(unsignedTx, eth1PrivKey);
  }
}
