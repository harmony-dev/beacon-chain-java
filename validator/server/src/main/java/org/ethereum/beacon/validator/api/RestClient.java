package org.ethereum.beacon.validator.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.validator.api.model.AttestationSubmit;
import org.ethereum.beacon.validator.api.model.BlockData;
import org.ethereum.beacon.validator.api.model.BlockSubmit;
import org.ethereum.beacon.validator.api.model.ForkResponse;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;

public class RestClient implements ValidatorClient {
  private final Client client;
  private final String url;

  public RestClient(String url) {
    this.client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
    this.url = url;
  }

  @Override
  public String getVersion() {
    return client
        .target(url)
        .path("/node/version")
        .request(MediaType.APPLICATION_JSON)
        .get(String.class);
  }

  @Override
  public Long getGenesisTime() {
    return client
        .target(url)
        .path("/node/genesis_time")
        .request(MediaType.APPLICATION_JSON)
        .get(Long.class);
  }

  @Override
  public SyncingResponse getSyncing() {
    return client
        .target(url)
        .path("/node/syncing")
        .request(MediaType.APPLICATION_JSON)
        .get(SyncingResponse.class);
  }

  @Override
  public ValidatorDutiesResponse getValidatorDuties(Long epoch, String[] pubKeys) {
    return client
        .target(url)
        .path("/validator/duties")
        .queryParam("validator_pubkeys", (Object[]) pubKeys)
        .queryParam("epoch", epoch)
        .request(MediaType.APPLICATION_JSON)
        .get(ValidatorDutiesResponse.class);
  }

  @Override
  public BeaconBlock getBlock(BigInteger slot, String randaoReveal, SpecConstants constants) {
    return client
        .target(url)
        .path("/validator/block")
        .queryParam("slot", slot)
        .queryParam("randao_reveal", randaoReveal)
        .request(MediaType.APPLICATION_JSON)
        .get(BlockData.class)
        .createBlock(constants);
  }

  @Override
  public Response postBlock(SignedBeaconBlock block) {
    BlockSubmit blockSubmit = BlockSubmit.fromBeaconBlock(block);
    return client
        .target(url)
        .path("/validator/block")
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(blockSubmit, MediaType.APPLICATION_JSON));
  }

  @Override
  public BlockData.BlockBodyData.IndexedAttestationData getAttestation(
      String validatorPubkey, Long pocBit, BigInteger slot, Integer index) {
    return client
        .target(url)
        .path("/validator/attestation")
        .queryParam("validator_pubkey", validatorPubkey)
        .queryParam("poc_bit", pocBit)
        .queryParam("slot", slot)
        .queryParam("index", index)
        .request(MediaType.APPLICATION_JSON)
        .get(BlockData.BlockBodyData.IndexedAttestationData.class);
  }

  @Override
  public Response postAttestation(IndexedAttestation attestation) {
    AttestationSubmit attestationSubmit = AttestationSubmit.fromAttestation(attestation);
    return client
        .target(url)
        .path("/validator/attestation")
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(attestationSubmit, MediaType.APPLICATION_JSON));
  }

  @Override
  public ForkResponse getFork() {
    return client
        .target(url)
        .path("/node/fork")
        .request(MediaType.APPLICATION_JSON)
        .get(ForkResponse.class);
  }
}
