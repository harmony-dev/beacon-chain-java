package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.main.Configuration;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.action.ActionSimulate;
import org.junit.Test;
import tech.pegasys.artemis.util.uint.UInt64;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ConfigBuilderTest {
  @Test
  public void test1() {
    ConfigBuilder configBuilder = new ConfigBuilder(MainConfig.class);
    File testYamlConfig = new File(getClass().getClassLoader().getResource("config.yml").getFile());

    configBuilder.addYamlConfig(testYamlConfig);
    MainConfig unmodified = (MainConfig) configBuilder.build();
    assertEquals("file://db", unmodified.getConfig().getDb());
    assertEquals(3, unmodified.getPlan().getValidator().size());
    ActionSimulate actionSimulate = ((ActionSimulate) unmodified.getPlan().getValidator().get(2));
    assertEquals(4, (long) actionSimulate.getCount());

    MainConfig config2 = new MainConfig();
    Configuration configPart = new Configuration();
    configPart.setDb("file://second/path");
    config2.setConfig(configPart);
    configBuilder.addConfig(config2);
    MainConfig merged = (MainConfig) configBuilder.build();
    assertEquals("file://second/path", merged.getConfig().getDb());
    assertEquals(3, merged.getPlan().getValidator().size());
    assertEquals("ethereumj", merged.getConfig().getValidator().getContract().get("handler"));

    configBuilder.addConfigOverride("config.db", "file://test-db");
    configBuilder.addConfigOverride("config.validator.contract.handler", "unknown");
    MainConfig overrided = (MainConfig) configBuilder.build();
    assertEquals("file://test-db", overrided.getConfig().getDb());
    assertEquals(3, overrided.getPlan().getValidator().size());
    assertEquals("unknown", overrided.getConfig().getValidator().getContract().get("handler"));
  }

  @Test
  public void testChainSpec() {
    ConfigBuilder configBuilder = new ConfigBuilder(SpecConstantsData.class);
    File testYamlConfig =
        new File(getClass().getClassLoader().getResource("chainSpec.yml").getFile());

    configBuilder.addYamlConfig(testYamlConfig);
    SpecConstantsData unmodified = (SpecConstantsData) configBuilder.build();
    SpecConstants specConstants = unmodified.build();

    SpecConstants specConstantsDefault = SpecConstants.DEFAULT;
    assertEquals(specConstantsDefault.getGenesisEpoch(), specConstants.getGenesisEpoch());
    assertEquals(specConstantsDefault.getEmptySignature(), specConstants.getEmptySignature());
    assertEquals(specConstantsDefault.getSlotsPerEpoch(), specConstants.getSlotsPerEpoch());
    assertEquals(specConstantsDefault.getSecondsPerSlot(), specConstants.getSecondsPerSlot());

    configBuilder.addConfigOverride("timeParameters.SECONDS_PER_SLOT", "10");
    SpecConstantsData overriden = (SpecConstantsData) configBuilder.build();
    SpecConstants specConstants2 = overriden.build();
    assertNotEquals(specConstantsDefault.getSecondsPerSlot(), specConstants2.getSecondsPerSlot());
    assertEquals(UInt64.valueOf(10), specConstants2.getSecondsPerSlot());
  }
}
