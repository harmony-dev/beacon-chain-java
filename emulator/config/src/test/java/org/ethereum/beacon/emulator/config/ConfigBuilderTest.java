package org.ethereum.beacon.emulator.config;

import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.emulator.config.chainspec.ChainSpecData;
import org.ethereum.beacon.emulator.config.main.Configuration;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.action.ActionEmulate;
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
    assertEquals("file://chainSpec.json", unmodified.getConfig().getChainSpec());
    assertEquals(3, unmodified.getPlan().getValidator().size());
    assertEquals(4, ((ActionEmulate) unmodified.getPlan().getValidator().get(2)).getCount());

    MainConfig config2 = new MainConfig();
    Configuration configPart = new Configuration();
    configPart.setDb("file://second/path");
    config2.setConfig(configPart);
    configBuilder.addConfig(config2);
    MainConfig merged = (MainConfig) configBuilder.build();
    assertEquals("file://second/path", merged.getConfig().getDb());
    assertEquals("file://chainSpec.json", merged.getConfig().getChainSpec());
    assertEquals(3, merged.getPlan().getValidator().size());
    assertEquals("ethereumj", merged.getConfig().getValidator().getContract().get("handler"));

    configBuilder.addConfigOverride("config.db", "file://test-db");
    configBuilder.addConfigOverride("config.validator.contract.handler", "unknown");
    MainConfig overrided = (MainConfig) configBuilder.build();
    assertEquals("file://test-db", overrided.getConfig().getDb());
    assertEquals("file://chainSpec.json", overrided.getConfig().getChainSpec());
    assertEquals(3, overrided.getPlan().getValidator().size());
    assertEquals("unknown", overrided.getConfig().getValidator().getContract().get("handler"));
  }

  @Test
  public void testChainSpec() {
    ConfigBuilder configBuilder = new ConfigBuilder(ChainSpecData.class);
    File testYamlConfig =
        new File(getClass().getClassLoader().getResource("chainSpec.yml").getFile());

    configBuilder.addYamlConfig(testYamlConfig);
    ChainSpecData unmodified = (ChainSpecData) configBuilder.build();
    ChainSpec chainSpec = unmodified.build();

    ChainSpec chainSpecDefault = ChainSpec.DEFAULT;
    assertEquals(chainSpecDefault.getGenesisEpoch(), chainSpec.getGenesisEpoch());
    assertEquals(chainSpecDefault.getEmptySignature(), chainSpec.getEmptySignature());
    assertEquals(chainSpecDefault.getEpochLength(), chainSpec.getEpochLength());
    assertEquals(chainSpecDefault.getSlotDuration(), chainSpec.getSlotDuration());

    configBuilder.addConfigOverride("timeParameters.SLOT_DURATION", "10");
    ChainSpecData overriden = (ChainSpecData) configBuilder.build();
    ChainSpec chainSpec2 = overriden.build();
    assertNotEquals(chainSpecDefault.getSlotDuration(), chainSpec2.getSlotDuration());
    assertEquals(UInt64.valueOf(10), chainSpec2.getSlotDuration());
  }
}
