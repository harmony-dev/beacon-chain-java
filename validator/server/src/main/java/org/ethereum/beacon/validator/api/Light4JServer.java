package org.ethereum.beacon.validator.api;

import com.networknt.server.Server;
import org.ethereum.beacon.chain.observer.ObservableStateProcessor;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

public class Light4JServer implements RestServer {

  public Light4JServer(ObservableStateProcessor stateProcessor) {
    Server.start();
    Flux.from(stateProcessor.getObservableStateStream())
        .subscribe(
            observableBeaconState -> {
              if (ResourceFactory.getGenesis_time().getTime() == null) {
                ResourceFactory.getGenesis_time()
                    .setTime(
                        observableBeaconState.getLatestSlotState().getGenesisTime().getValue());
              }
            });
    Properties props = loadResources("rest-server.properties", this.getClass().getClassLoader());
    final String version = props.getProperty("versionNumber");
    ResourceFactory.getVersion().setVersion(String.format("Beacon Chain Java %s", version));
  }

  private Properties loadResources(final String name, final ClassLoader classLoader) {
    try {
      final Enumeration<URL> systemResources =
          (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
              .getResources(name);
      Properties props = new Properties();
      props.load(systemResources.nextElement().openStream());

      return props;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void shutdown() {
    Server.shutdown();
  }
}
