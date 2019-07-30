package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
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
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.PartiallyFailedException;
import org.ethereum.beacon.validator.api.model.AttestationSubmit;
import org.ethereum.beacon.wire.WireApiSub;
import org.ethereum.beacon.wire.sync.SyncManager;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AttestationSubmitController extends SyncRestController {
  private final WireApiSub wireApiSub;
  private final BeaconChainSpec spec;
  private ObservableBeaconState observableBeaconState = null;

  public AttestationSubmitController(
      SyncManager syncManager,
      WireApiSub wireApiSub,
      ObservableStateProcessor stateProcessor,
      BeaconChainSpec spec) {
    super(syncManager);
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              this.observableBeaconState = observableBeaconState;
            });
    this.wireApiSub = wireApiSub;
    this.spec = spec;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return processPostRequestImpl(this::acceptAttestationSubmit);
  }

  private Optional<Throwable> acceptAttestationSubmit(String body) {
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
