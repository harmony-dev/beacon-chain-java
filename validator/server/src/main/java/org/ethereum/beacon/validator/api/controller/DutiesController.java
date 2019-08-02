package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.InvalidInputException;
import org.ethereum.beacon.validator.api.NotAcceptableInputException;
import org.ethereum.beacon.validator.api.ValidatorDutiesService;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import reactor.core.publisher.Flux;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DutiesController extends SyncRestController {
  private final BeaconChainSpec spec;
  private final ValidatorDutiesService service;
  private ObservableBeaconState observableBeaconState = null;

  public DutiesController(
      ObservableStateProcessor stateProcessor,
      ValidatorDutiesService service,
      SyncManager syncManager,
      BeaconChainSpec spec) {
    super(syncManager);
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(this::updateState);
    this.spec = spec;
    this.service = service;
  }

  private synchronized void updateState(ObservableBeaconState observableBeaconState) {
    this.observableBeaconState = observableBeaconState;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return processGetRequestImpl(this::produceValidatorDutiesResponse);
  }

  private synchronized Object produceValidatorDutiesResponse(HttpServerRequest request) {
    try {
      MultiMap params = request.params();
      EpochNumber epoch;
      BeaconStateEx stateEx = observableBeaconState.getLatestSlotState();
      if (params.contains("epoch")) {
        epoch = EpochNumber.castFrom(UInt64.valueOf(getParamString("epoch", params)));
      } else { // Epoch is not required
        epoch = spec.get_current_epoch(stateEx).increment();
      }

      List<BLSPubkey> pubKeys =
          getParamStringList("validator_pubkeys", params).stream()
              .map(Bytes48::fromHexStringStrict)
              .map(BLSPubkey::wrap)
              .collect(Collectors.toList());
      if (!epoch.lessEqual(spec.get_current_epoch(stateEx).increment())) {
        throw new NotAcceptableInputException("Couldn't provide duties for requested epoch");
      }
      Map<SlotNumber, Pair<ValidatorIndex, List<ShardCommittee>>> validatorDuties =
          service.getValidatorDuties(stateEx, epoch);

      List<ValidatorDutiesResponse.ValidatorDuty> responseList = new ArrayList<>();
      for (BLSPubkey pubkey : pubKeys) {
        ValidatorIndex validatorIndex = spec.get_validator_index_by_pubkey(stateEx, pubkey);
        Triplet<BigInteger, Integer, BigInteger> duty =
            service.findDutyForValidator(validatorIndex, validatorDuties);
        if (duty.getValue1() != null) {
          ValidatorDutiesResponse.ValidatorDuty dutyResponse =
              new ValidatorDutiesResponse.ValidatorDuty();
          dutyResponse.setValidatorPubkey(pubkey.toHexString());
          dutyResponse.setBlockProposalSlot(duty.getValue0());
          dutyResponse.setAttestationSlot(duty.getValue2());
          dutyResponse.setAttestationShard(duty.getValue1());
          responseList.add(dutyResponse);
        }
      }

      return new ValidatorDutiesResponse(responseList);
    } catch (IllegalArgumentException ex) {
      throw new InvalidInputException(ex);
    } catch (InvalidInputException | NotAcceptableInputException ex) {
      throw ex;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
