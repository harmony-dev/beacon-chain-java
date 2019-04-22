package org.ethereum.beacon.benchmaker;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconChainSpec.Builder;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import picocli.CommandLine;
import picocli.CommandLine.RunLast;

@CommandLine.Command(
    description = "Benchmark tool for beacon chain spec",
    name = "benchmaker",
    version = "benchmaker " + Benchmaker.VERSION,
    mixinStandardHelpOptions = true)
public class Benchmaker implements Runnable {

  static final String VERSION = "0.1.0";

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  @CommandLine.Option(
      names = {"--epochs"},
      paramLabel = "epochs",
      description =
          "Number of epochs passed to a benchmark session.\nNote: genesis epoch is not counted in a session.",
      defaultValue = "16")
  private Integer epochs;

  @CommandLine.Option(
      names = {"--validators"},
      paramLabel = "validators",
      description = "Number of validators.",
      defaultValue = "64")
  private Integer validators;

  @CommandLine.Option(
      names = {"--no-bls"},
      paramLabel = "no-bls",
      description = "Turns off BLS.")
  private Boolean noBls = false;

  @CommandLine.Option(
      names = {"--no-cache"},
      paramLabel = "no-cache",
      description = "Turns off caches used in the spec execution.")
  private Boolean noCache = false;

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
    SpecData specData =
        new ConfigBuilder<>(SpecData.class)
            .addYamlConfigFromResources("/config/spec-constants.yml")
            .build();
    SpecConstants constants = SpecBuilder.buildSpecConstants(specData.getSpecConstants());

    BeaconChainSpec.Builder specBuilder =
        new Builder()
            .withConstants(constants)
            .withDefaultHashFunction()
            .withDefaultHasher(constants)
            .withBlsVerify(!noBls)
            .withCache(!noCache)
            .withBlsVerifyProofOfPossession(false);

    new BenchmarkRunner(epochs, validators, specBuilder).run();
  }
}
