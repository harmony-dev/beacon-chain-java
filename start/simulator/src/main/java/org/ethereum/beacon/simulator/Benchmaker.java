package org.ethereum.beacon.simulator;

import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import picocli.CommandLine;
import picocli.CommandLine.RunLast;

@CommandLine.Command(
    description = "Beacon chain benchmark runner",
    name = "benchmaker",
    version = "benchmaker " + Benchmaker.VERSION,
    mixinStandardHelpOptions = true)
public class Benchmaker implements Runnable {

  static final String VERSION = "0.1.0";

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  public static void main(String[] args) {
    try {
      CommandLine commandLine = new CommandLine(new Benchmaker());
      commandLine.setCaseInsensitiveEnumValuesAllowed(true);
      commandLine.parseWithHandlers(
          new RunLast().andExit(SUCCESS_EXIT_CODE),
          CommandLine.defaultExceptionHandler().andExit(ERROR_EXIT_CODE),
          args);
    } catch (Exception e) {
      System.out.println(String.format((char) 27 + "[31m" + "FATAL ERROR: %s", e.getMessage()));
    }
  }

  @Override
  public void run() {
    int epochCount = 1;
    int validatorCount = 8;
    boolean blsEnabled = true;

    SpecData specData =
        new ConfigBuilder<>(SpecData.class)
            .addYamlConfigFromResources("/config/spec-constants.yml")
            .build();
    specData.getSpecHelpersOptions().setBlsVerify(blsEnabled);
    specData.getSpecHelpersOptions().setBlsSign(blsEnabled);
    specData.getSpecHelpersOptions().setBlsVerifyProofOfPossession(false);
    specData.getSpecConstants().getTimeParameters().setSLOTS_PER_EPOCH("8");
    SpecBuilder specBuilder = new SpecBuilder().withSpec(specData);

    new BenchmarkRunner(epochCount, validatorCount, specBuilder, blsEnabled).run();
  }
}
