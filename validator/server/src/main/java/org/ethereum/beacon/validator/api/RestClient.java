package org.ethereum.beacon.validator.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.ethereum.beacon.validator.api.model.AttestationSubmit;
import org.ethereum.beacon.validator.api.model.BlockData;
import org.ethereum.beacon.validator.api.model.BlockSubmit;
import org.ethereum.beacon.validator.api.model.ForkResponse;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigInteger;

public class RestClient {
  private final Client client;
  private final String url;

  public RestClient(String url) {
    this.client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
    this.url = url;
  }

  public VersionResponse getVersion() {
    return client
        .target(url)
        .path("/node/version")
        .request(MediaType.APPLICATION_JSON)
        .get(VersionResponse.class);
  }

  public TimeResponse getGenesisTime() {
    return client
        .target(url)
        .path("/node/genesis_time")
        .request(MediaType.APPLICATION_JSON)
        .get(TimeResponse.class);
  }

  public SyncingResponse getSyncing() {
    return client
        .target(url)
        .path("/node/syncing")
        .request(MediaType.APPLICATION_JSON)
        .get(SyncingResponse.class);
  }

  public ValidatorDutiesResponse getValidatorDuties(Long epoch, String[] pubKeys) {
    return client
        .target(url)
        .path("/validator/duties")
        .queryParam("validator_pubkeys", (Object[]) pubKeys)
        .queryParam("epoch", epoch)
        .request(MediaType.APPLICATION_JSON)
        .get(ValidatorDutiesResponse.class);
  }

  public BlockData getBlock(BigInteger slot, String randaoReveal) {
    return client
        .target(url)
        .path("/validator/block")
        .queryParam("slot", slot)
        .queryParam("randao_reveal", randaoReveal)
        .request(MediaType.APPLICATION_JSON)
        .get(BlockData.class);
  }

  public Response postBlock(BlockData blockData) {
    BlockSubmit blockSubmit = new BlockSubmit(blockData);
    return client
        .target(url)
        .path("/validator/block")
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(blockSubmit, MediaType.APPLICATION_JSON));
  }

  public BlockData.BlockBodyData.IndexedAttestationData getAttestation(
      String validatorPubkey, Long pocBit, BigInteger slot, Integer shard) {
    return client
        .target(url)
        .path("/validator/attestation")
        .queryParam("validator_pubkey", validatorPubkey)
        .queryParam("poc_bit", pocBit)
        .queryParam("slot", slot)
        .queryParam("shard", shard)
        .request(MediaType.APPLICATION_JSON)
        .get(BlockData.BlockBodyData.IndexedAttestationData.class);
  }

  public Response postAttestation(BlockData.BlockBodyData.IndexedAttestationData attestationData) {
    AttestationSubmit attestationSubmit = new AttestationSubmit(attestationData);
    return client
        .target(url)
        .path("/validator/attestation")
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(attestationSubmit, MediaType.APPLICATION_JSON));
  }

  public ForkResponse getFork() {
    return client
        .target(url)
        .path("/node/fork")
        .request(MediaType.APPLICATION_JSON)
        .get(ForkResponse.class);
  }
}