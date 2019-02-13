package org.ethereum.beacon;

import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
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
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.db.InMemoryDatabase;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.BeaconChainValidator;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterImpl;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerImpl;
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

  public Launcher(SpecHelpers specHelpers, DepositContract depositContract,
      KeyPair validatorSig, WireApi wireApi) {
    this.specHelpers = specHelpers;
    this.depositContract = depositContract;
    this.validatorSig = validatorSig;
    this.wireApi = wireApi;

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
    beaconChainStorage = BeaconChainStorageFactory.get().create(db);

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
        beaconChainStorage);
    beaconChain.init();

    slotTicker = new SlotTicker(specHelpers, beaconChain.getRecentlyProcessed().getState());
    slotTicker.start();

    observableStateProcessor = new ObservableStateProcessorImpl(
        beaconChainStorage,
        slotTicker.getTickerStream(),
        wireApi.inboundAttestationsStream(),
        beaconChain.getBlockStatesStream(),
        specHelpers,
        perSlotTransition,
        perEpochTransition);
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
        observableStateProcessor.getObservableStateStream());
    beaconChainValidator.start();

    Flux.from(beaconChainValidator.getProposedBlocksStream()).subscribe(wireApi::sendProposedBlock);
    Flux.from(beaconChainValidator.getAttestationsStream()).subscribe(wireApi::sendAttestation);

    Flux.from(wireApi.inboundBlocksStream()).subscribe(beaconChain::insert);
  }
}
