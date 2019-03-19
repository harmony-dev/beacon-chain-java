package org.ethereum.beacon.simulator.command;

import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import picocli.CommandLine;

@CommandLine.Command(
    name = "spec",
    description = "Prints beacon chain constants defined by the spec")
public class PrintSpec implements Runnable {

  @Override
  public void run() {
    SpecData spec =
        new ConfigBuilder<>(SpecData.class)
            .addYamlConfigFromResources("/config/spec-constants.yml")
            .build();
    System.out.println(new YamlPrinter(spec).getString());
  }
}
