package org.ethereum.beacon.validator.api;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.ethereum.beacon.validator.api.model.TimeResponse;
import org.ethereum.beacon.validator.api.model.VersionResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

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
}