package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.emulator.config.version.Config;
import org.ethereum.beacon.emulator.config.version.ConfigV1;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class ConfigReaderTest {
  @Test
  public void test1() {
    ConfigBuilder configBuilder = new ConfigBuilder();
    File testYamlConfig = new File(getClass().getClassLoader().getResource("config.yml").getFile());

    configBuilder.addYamlConfig(testYamlConfig);
    Config unmodified = configBuilder.build();
    assertEquals(1, unmodified.getVersion());
    ConfigV1 unmodifiedV1 = (ConfigV1) unmodified;
    assertEquals("file://db", unmodifiedV1.getConfig().getDb());
    assertEquals("file://chainSpec.json", unmodifiedV1.getConfig().getChainSpec());
    assertEquals(3, unmodifiedV1.getPlan().getValidator().size());
    assertEquals(
        4,
        ((ConfigV1.PlanPart.ActionEmulate) unmodifiedV1.getPlan().getValidator().get(2))
            .getCount());

    ConfigV1 config2 = new ConfigV1();
    ConfigV1.ConfigPart configPart = config2.new ConfigPart();
    configPart.setDb("file://second/path");
    config2.setConfig(configPart);
    configBuilder.addConfig(config2);
    ConfigV1 merged = (ConfigV1) configBuilder.build();
    assertEquals("file://second/path", merged.getConfig().getDb());
    assertEquals("file://chainSpec.json", merged.getConfig().getChainSpec());
    assertEquals(3, merged.getPlan().getValidator().size());
    assertEquals("ethereumj", merged.getConfig().getValidator().getContract().get("handler"));

    configBuilder.addConfigOverride("config.db", "file://test-db");
    configBuilder.addConfigOverride("config.validator.contract.handler", "unknown");
    ConfigV1 overrided = (ConfigV1) configBuilder.build();
    assertEquals("file://test-db", overrided.getConfig().getDb());
    assertEquals("file://chainSpec.json", overrided.getConfig().getChainSpec());
    assertEquals(3, overrided.getPlan().getValidator().size());
    assertEquals("unknown", overrided.getConfig().getValidator().getContract().get("handler"));
  }
}
