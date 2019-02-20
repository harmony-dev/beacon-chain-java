package org.ethereum.beacon;

import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.ProposedBlockProcessor;
import org.ethereum.beacon.chain.ProposedBlockProcessorImpl;
import org.ethereum.beacon.chain.SlotTicker;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.chain.observer.ObservableStateProcessorImpl;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.BeaconChainStorageFactory;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.BeaconChainValidator;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerImpl;
import org.ethereum.beacon.wire.WireApi;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Launcher {
  private final SpecHelpers specHelpers;
  private final DepositContract depositContract;
  private final BLS381.KeyPair validatorSig;
  private final WireApi wireApi;

  InMemoryDatabase db;
  BeaconChainStorage beaconChainStorage;
  MutableBeaconChain beaconChain;
  SlotTicker slotTicker;
  ObservableStateProcessor observableStateProcessor;
  BeaconChainProposer beaconChainProposer;
  BeaconChainAttesterImpl beaconChainAttester;
  BeaconChainValidator beaconChainValidator;
  BeaconChainStorageFactory storageFactory;
  Schedulers schedulers;

  public Launcher(
      SpecHelpers specHelpers,
      DepositContract depositContract,
      KeyPair validatorSig,
      WireApi wireApi,
      BeaconChainStorageFactory storageFactory,
      Schedulers schedulers) {

    this.specHelpers = specHelpers;
    this.depositContract = depositContract;
    this.validatorSig = validatorSig;
    this.wireApi = wireApi;
    this.storageFactory = storageFactory;
    this.schedulers = schedulers;

    if (depositContract != null) {
      Mono.from(depositContract.getChainStartMono()).subscribe(this::chainStarted);
    }
  }

  void chainStarted(ChainStart chainStartEvent) {
    InitialStateTransition initialTransition = new InitialStateTransition(chainStartEvent, specHelpers);
    PerSlotTransition perSlotTransition = new PerSlotTransition(specHelpers);
    PerBlockTransition perBlockTransition = new PerBlockTransition(specHelpers);
    PerEpochTransition perEpochTransition = new PerEpochTransition(specHelpers);

    db = new InMemoryDatabase();
    beaconChainStorage = storageFactory.create(db);

    // TODO
    BeaconBlockVerifier blockVerifier = (block, state) -> VerificationResult.PASSED;
    // TODO
    BeaconStateVerifier stateVerifier = (block, state) -> VerificationResult.PASSED;

    beaconChain = new DefaultBeaconChain(
        specHelpers,
        initialTransition,
        perSlotTransition,
        perBlockTransition,
        perEpochTransition,
        blockVerifier,
        stateVerifier,
        beaconChainStorage,
        schedulers);
    beaconChain.init();

    slotTicker =
        new SlotTicker(specHelpers, beaconChain.getRecentlyProcessed().getState(), schedulers);
    slotTicker.start();

    DirectProcessor<Attestation> allAttestations = DirectProcessor.create();
    Flux.from(wireApi.inboundAttestationsStream())
        .publishOn(schedulers.reactorEvents())
        .subscribe(allAttestations);

    observableStateProcessor = new ObservableStateProcessorImpl(
        beaconChainStorage,
        slotTicker.getTickerStream(),
        allAttestations,
        beaconChain.getBlockStatesStream(),
        specHelpers,
        perSlotTransition,
        perEpochTransition,
        schedulers);
    observableStateProcessor.start();

    beaconChainProposer = new BeaconChainProposerImpl(specHelpers,
        specHelpers.getChainSpec(), perBlockTransition, perEpochTransition, depositContract);
    beaconChainAttester = new BeaconChainAttesterImpl(specHelpers,
        specHelpers.getChainSpec());

    beaconChainValidator = new BeaconChainValidator(
        BLSPubkey.wrap(validatorSig.getPublic().getEncodedBytes()),
        beaconChainProposer,
        beaconChainAttester,
        specHelpers,
        (msgHash, domain) -> BLSSignature.wrap(
            BLS381.sign(MessageParameters.create(msgHash, domain), validatorSig).getEncoded()),
        observableStateProcessor.getObservableStateStream(),
        schedulers);
    beaconChainValidator.start();

    ProposedBlockProcessor proposedBlocksProcessor = new ProposedBlockProcessorImpl(
        beaconChain, schedulers);
    Flux.from(beaconChainValidator.getProposedBlocksStream())
        .subscribe(proposedBlocksProcessor::newBlockProposed);
    Flux.from(proposedBlocksProcessor.processedBlocksStream())
        .subscribe(wireApi::sendProposedBlock);

    Flux.from(beaconChainValidator.getAttestationsStream()).subscribe(wireApi::sendAttestation);
    Flux.from(beaconChainValidator.getAttestationsStream()).subscribe(allAttestations);

    Flux.from(wireApi.inboundBlocksStream())
        .publishOn(schedulers.reactorEvents())
        .subscribe(beaconChain::insert);
  }
}
