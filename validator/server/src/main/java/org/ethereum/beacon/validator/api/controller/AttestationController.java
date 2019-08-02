package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.spec.SpecCommons;
import org.ethereum.beacon.consensus.verifier.operation.AttestationVerifier;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.PartiallyFailedException;
import org.ethereum.beacon.validator.api.ValidatorDutiesService;
import org.ethereum.beacon.validator.api.convert.BeaconBlockConverter;
import org.ethereum.beacon.validator.api.model.AttestationSubmit;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AttestationController extends SyncRestController {
  private final ValidatorDutiesService service;
  private final WireApiSub wireApiSub;
  private final BeaconChainSpec spec;
  private ObservableBeaconState observableBeaconState = null;

  public AttestationController(
      ObservableStateProcessor stateProcessor,
      WireApiSub wireApiSub,
      ValidatorDutiesService service,
      SyncManager syncManager,
      BeaconChainSpec spec) {
    super(syncManager);
    Flux.from(stateProcessor.getObservableStateStream()).subscribe(this::updateState);
    this.service = service;
    this.wireApiSub = wireApiSub;
    this.spec = spec;
  }

  private synchronized void updateState(ObservableBeaconState observableBeaconState) {
    this.observableBeaconState = observableBeaconState;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return processGetRequestImpl(this::produceAttestationResponse);
  }

  @Override
  public Handler<RoutingContext> postHandler() {
    return processPostRequestImpl(this::acceptAttestationSubmit);
  }

  private synchronized Object produceAttestationResponse(HttpServerRequest request) {
    try {
      MultiMap params = request.params();
      SlotNumber slot =
          SlotNumber.castFrom(UInt64.valueOf(ControllerUtils.getParamString("slot", params)));
      BLSPubkey validatorPubkey =
          BLSPubkey.wrap(
              Bytes48.fromHexStringStrict(
                  ControllerUtils.getParamString("validator_pubkey", params)));
      Long pocBit =
          Long.valueOf(
              ControllerUtils.getParamString(
                  "poc_bit", params)); // XXX: Proof of custody is a stub at Phase 0
      ShardNumber shard =
          ShardNumber.of(UInt64.valueOf(ControllerUtils.getParamString("shard", params)));

      ValidatorIndex validatorIndex =
          spec.get_validator_index_by_pubkey(
              observableBeaconState.getLatestSlotState().createMutableCopy(), validatorPubkey);
      Attestation attestation =
          service.prepareAttestation(slot, validatorIndex, shard, observableBeaconState);
      IndexedAttestation indexedAttestation =
          spec.get_indexed_attestation(
              observableBeaconState.getLatestSlotState().createMutableCopy(), attestation);
      return BeaconBlockConverter.presentIndexedAttestation(indexedAttestation);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private synchronized Optional<Throwable> acceptAttestationSubmit(String body) {
    try {
      final AttestationSubmit submitData = Json.decodeValue(body, AttestationSubmit.class);
      IndexedAttestation indexedAttestation = submitData.createAttestation(spec.getConstants());
      // Verification
      MutableBeaconState state = observableBeaconState.getLatestSlotState().createMutableCopy();
      List<ValidatorIndex> committee =
          spec.get_crosslink_committee(
              state,
              indexedAttestation.getData().getTarget().getEpoch(),
              indexedAttestation.getData().getCrosslink().getShard());
      Bitlist bitlist =
          Bitlist.of(
              committee.size(),
              indexedAttestation.getCustodyBit0Indices().listCopy().stream()
                  .map(ValidatorIndex::intValue)
                  .collect(Collectors.toList()),
              spec.getConstants().getMaxValidatorsPerCommittee().intValue());
      Attestation attestation =
          new Attestation(
              bitlist,
              indexedAttestation.getData(),
              bitlist,
              indexedAttestation.getSignature(),
              spec.getConstants());
      try {
        if (new AttestationVerifier(spec).verify(attestation, state).isPassed()) {
          spec.process_attestation(state, attestation);
        } else {
          throw new PartiallyFailedException("Verification not passed for attestation");
        }
      } catch (SpecCommons.SpecAssertionFailed | IllegalArgumentException ex) {
        throw new PartiallyFailedException(ex);
      } finally {
        // Broadcast
        wireApiSub.sendAttestation(attestation);
      }
      return Optional.empty();
    } catch (PartiallyFailedException e) {
      return Optional.of(e);
    } catch (AssertionError ex) {
      return Optional.of(new InvalidInputException(ex));
    } catch (Exception e) {
      return Optional.of(new RuntimeException(e));
    }
  }
}
