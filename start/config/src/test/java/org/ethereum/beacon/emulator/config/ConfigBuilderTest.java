package org.ethereum.beacon.emulator.config;

import java.util.Collections;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.main.Configuration;
import org.ethereum.beacon.emulator.config.main.MainConfig;
import org.ethereum.beacon.emulator.config.main.action.ActionRun;
import org.ethereum.beacon.emulator.config.main.plan.GeneralPlan;
import org.junit.Test;
import tech.pegasys.artemis.util.uint.UInt64;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ConfigBuilderTest {

  @Test
  public void test0() throws Exception {
    MainConfig mainConfig = new MainConfig();
    GeneralPlan plan = new GeneralPlan();
    mainConfig.setPlan(plan);
    plan.setSync(Collections.singletonList(new ActionRun()));
    System.out.println(new YamlPrinter(mainConfig).getString());
  }

  @Test
  public void test1() {
    ConfigBuilder<MainConfig> configBuilder = new ConfigBuilder<>(MainConfig.class);
    File testYamlConfig = new File(getClass().getClassLoader().getResource("config.yml").getFile());

    configBuilder.addYamlConfig(testYamlConfig);
    MainConfig unmodified = configBuilder.build();
    assertEquals("file://db", unmodified.getConfig().getDb());
    assertEquals(2, ((GeneralPlan) unmodified.getPlan()).getValidator().size());

    MainConfig config2 = new MainConfig();
    Configuration configPart = new Configuration();
    configPart.setDb("file://second/path");
    config2.setConfig(configPart);
    configBuilder.addConfig(config2);
    MainConfig merged = configBuilder.build();
    assertEquals("file://second/path", merged.getConfig().getDb());
    assertEquals(2, ((GeneralPlan) merged.getPlan()).getValidator().size());
    assertEquals("ethereumj", merged.getConfig().getValidator().getContract().get("handler"));

    configBuilder.addConfigOverride("config.db", "file://test-db");
    configBuilder.addConfigOverride("config.validator.contract.handler", "unknown");
    MainConfig overrided = configBuilder.build();
    assertEquals("file://test-db", overrided.getConfig().getDb());
    assertEquals(2, ((GeneralPlan) overrided.getPlan()).getValidator().size());
    assertEquals("unknown", overrided.getConfig().getValidator().getContract().get("handler"));
  }

  @Test
  public void testChainSpec() {
    ConfigBuilder<SpecData> configBuilder = new ConfigBuilder<>(SpecData.class);
    File testYamlConfig =
        new File(getClass().getClassLoader().getResource("chainSpec.yml").getFile());

    configBuilder.addYamlConfig(testYamlConfig);
    SpecData unmodified = configBuilder.build();
    SpecConstants specConstants = new SpecBuilder().buildSpecConstants(unmodified.getSpecConstants());

    SpecConstants specConstantsDefault = BeaconChainSpec.DEFAULT_CONSTANTS;
    assertEquals(specConstantsDefault.getGenesisEpoch(), specConstants.getGenesisEpoch());
    assertEquals(specConstantsDefault.getSlotsPerEpoch(), specConstants.getSlotsPerEpoch());
    assertEquals(specConstantsDefault.getSecondsPerSlot(), specConstants.getSecondsPerSlot());

    configBuilder.addConfigOverride("specConstants.timeParameters.SECONDS_PER_SLOT", "10");
    SpecData overriden = configBuilder.build();
    SpecConstants specConstants2 = new SpecBuilder().buildSpecConstants(overriden.getSpecConstants());
    assertNotEquals(specConstantsDefault.getSecondsPerSlot(), specConstants2.getSecondsPerSlot());
    assertEquals(UInt64.valueOf(10), specConstants2.getSecondsPerSlot());
  }
}
