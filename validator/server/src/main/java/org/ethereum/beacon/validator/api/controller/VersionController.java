package org.ethereum.beacon.validator.api.controller;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

public class VersionController extends RestController {
  private String version = null;

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

  private String produceVersionResponse() {
    if (version == null) {
      Properties props = loadResources("rest-server.properties", this.getClass().getClassLoader());
      final String version = props.getProperty("versionNumber");
      this.version = String.format("Beacon Chain Java %s", version);
    }
    return version;
  }

  @Override
  public Handler<RoutingContext> getHandler() {
    return doStringResponse(this::produceVersionResponse);
  }
}
