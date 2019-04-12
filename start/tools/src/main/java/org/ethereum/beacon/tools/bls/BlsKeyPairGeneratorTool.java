package org.ethereum.beacon.tools.bls;

import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.util.BlsKeyPairGenerator;
import picocli.CommandLine;
import picocli.CommandLine.RunLast;

@CommandLine.Command(
    description = "BLS12-381 key pair generator",
    name = "bls-generator",
    version = "bls-generator 0.1",
    mixinStandardHelpOptions = true)
public class BlsKeyPairGeneratorTool implements Runnable {

  @CommandLine.Option(
      names = {"--seed"},
      paramLabel = "value",
      description = "Integer value that is used as a seed.\nRandom seed is used if not specified.")
  private Long seed;

  @CommandLine.Option(
      names = {"--count"},
      paramLabel = "count",
      description = "Number of key pairs to be generated.\n16 by default.",
      defaultValue = "16")
  private Long count;

  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  public static void main(String[] args) {
    try {
      CommandLine commandLine = new CommandLine(new BlsKeyPairGeneratorTool());
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
    BlsKeyPairGenerator generator =
        seed == null ? BlsKeyPairGenerator.createWithoutSeed() : BlsKeyPairGenerator.create(seed);

    for (long i = 0; i < count; i++) {
      KeyPair keyPair = generator.next();
      System.out.println(keyPair.asString());
    }
  }
}
