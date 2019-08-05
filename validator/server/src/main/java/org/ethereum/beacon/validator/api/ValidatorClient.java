package org.ethereum.beacon.validator.api;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.validator.api.model.BlockData;
import org.ethereum.beacon.validator.api.model.ForkResponse;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;

import javax.ws.rs.core.Response;
import java.math.BigInteger;

/** The minimal set of endpoints to enable a working validator client */
public interface ValidatorClient {
  /* Get version string of the running beacon node */
  String getVersion();

  /* Get the genesis_time parameter from beacon node configuration */
  Long getGenesisTime();

  /* Poll to see if the the beacon node is syncing */
  SyncingResponse getSyncing();

  /* Get validator duties for the requested validators */
  ValidatorDutiesResponse getValidatorDuties(Long epoch, String[] pubKeys);

  /* Produce a new block, without signature */
  BeaconBlock getBlock(BigInteger slot, String randaoReveal, SpecConstants constants);

  /* Publish a signed block */
  Response postBlock(BeaconBlock block);

  /* Produce an attestation, without signature */
  BlockData.BlockBodyData.IndexedAttestationData getAttestation(
      String validatorPubkey, Long pocBit, BigInteger slot, Integer shard);

  /* Publish a signed attestation */
  Response postAttestation(IndexedAttestation attestation);

  /* Get fork information from running beacon node */
  ForkResponse getFork();
}
