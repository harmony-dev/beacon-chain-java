package org.ethereum.beacon.command;

import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import org.ethereum.beacon.emulator.config.chainspec.Spec;
import picocli.CommandLine;

@CommandLine.Command(
    name = "spec",
    description = "Prints beacon chain constants defined by the spec")
public class PrintSpec implements Runnable {

  @Override
  public void run() {
    Spec spec =
        new ConfigBuilder<>(Spec.class)
            .addYamlConfigFromResources("/config/spec-constants.yml")
            .build();
    System.out.println(new YamlPrinter(spec).getString());
  }
}
