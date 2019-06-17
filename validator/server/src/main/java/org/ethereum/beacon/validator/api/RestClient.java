package org.ethereum.beacon.validator.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.ethereum.beacon.validator.api.model.BlockData;
import org.ethereum.beacon.validator.api.model.SyncingResponse;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.ValidatorDutiesResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
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
}
