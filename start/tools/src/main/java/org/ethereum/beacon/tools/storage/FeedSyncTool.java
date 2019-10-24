package org.ethereum.beacon.tools.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.impl.SSZBeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.chain.storage.util.StorageUtils;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.StateTransitions;
import org.ethereum.beacon.consensus.TransitionType;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.PerBlockTransition;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsData;
import org.ethereum.beacon.emulator.config.chainspec.SpecConstantsDataMerged;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.emulator.config.chainspec.SpecHelpersData;
import org.ethereum.beacon.node.command.LogLevel;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An utility to import serialized blocks into a storage. Optionally, initializes a storage from a
 * genesis file.
 */
@CommandLine.Command(
    description = "Feed Sync tool",
    name = "feed-sync",
    version = "feed-sync 0.1",
    mixinStandardHelpOptions = true)
public class FeedSyncTool implements Runnable {
  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_EXIT_CODE = 1;

  @CommandLine.Option(
      names = {"--loglevel"},
      paramLabel = "level",
      defaultValue = "info",
      description = {"Log verbosity level: all, debug, info, error.", "info is set by default."})
  private LogLevel logLevel;

  @CommandLine.Option(
      names = "--db-path",
      paramLabel = "db-path",
      required = true,
      description = "")
  private String dbPrefix;

  @CommandLine.Option(
      names = "--spec-constants",
      paramLabel = "spec-constants",
      required = true,
      description = "Path to a spec constants file in yaml format (flat format)")
  private String specConstantsFile;

  @CommandLine.Option(
      names = "--initial-state",
      paramLabel = "initial-state",
      description = {"Path to an initial state file (SSZ format)"})
  private File initialStateFile;

  @CommandLine.Parameters(arity = "1..*", paramLabel = "file-or-dir")
  private File[] inputFiles;

  public static void main(String[] args) {
    try {
      CommandLine commandLine = new CommandLine(new FeedSyncTool());
      commandLine.setCaseInsensitiveEnumValuesAllowed(true);
      commandLine.parseWithHandlers(
          new CommandLine.RunLast().andExit(SUCCESS_EXIT_CODE),
          CommandLine.defaultExceptionHandler().andExit(ERROR_EXIT_CODE),
          args);
    } catch (Exception e) {
      e.printStackTrace();
      e.getCause().printStackTrace();
      System.out.println(String.format((char) 27 + "[31m" + "FATAL ERROR: %s", e.getMessage()));
    }
  }

  public LogLevel getLogLevel() {
    return logLevel;
  }

  public String getDbPrefix() {
    return dbPrefix;
  }

  public String getSpecConstantsFile() {
    return specConstantsFile;
  }

  public File getInitialStateFile() {
    return initialStateFile;
  }

  public File[] getInputFiles() {
    return inputFiles;
  }

  @Override
  public void run() {
    initLogging();

    BeaconChainSpec spec = createBeaconChainSpec(getSpecConstantsFile());

    Database db = Database.rocksDB(getDbPrefix(), 1L << 20);

    SerializerFactory ssz = SerializerFactory.createSSZ(spec.getConstants());
    SSZBeaconChainStorageFactory storageFactory =
        new SSZBeaconChainStorageFactory(spec.getObjectHasher(), ssz);

    BeaconChainStorage chainStorage = storageFactory.create(db);

    if (chainStorage.getTupleStorage().isEmpty()) {
      Optional<File> genesisFile = findStateFile(getInitialStateFile(), getInputFiles());
      if (!genesisFile.isPresent()) {
        throw new IllegalArgumentException("If storage is empty, genesis.ssz should be supplied");
      }

      BeaconStateExImpl initialState =
          new BeaconStateExImpl(
              ssz.getDeserializer(BeaconStateImpl.class).apply(readFile(genesisFile.get())),
              TransitionType.INITIAL);
      StorageUtils.initializeStorage(chainStorage, spec, initialState);
      chainStorage.commit();
    }

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    ControlledSchedulers schedulers = controlledSchedulers.createNew("v1");

    DefaultBeaconChain beaconChain = createBeaconChain(spec, chainStorage, schedulers);

    List<File> files =
        Arrays.asList(getInputFiles()).stream()
            .flatMap(
                file ->
                    file.isDirectory()
                        ? Arrays.asList(file.listFiles()).stream().sorted()
                        : Stream.of(file))
            .filter(file -> file.getName().startsWith("block_") && file.getName().endsWith(".ssz"))
            .collect(Collectors.toList());

    for (File f : files) {
      System.out.print("importing " + f);
      try {
        BeaconBlock block = ssz.getDeserializer(BeaconBlock.class).apply(readFile(f));
        SlotNumber slot = block.getSlot();

        Time t = spec.get_slot_start_time(beaconChain.getRecentlyProcessed().getState(), slot);
        controlledSchedulers.setCurrentTime(t.getValue() * 1000 + 1);
        MutableBeaconChain.ImportResult result = beaconChain.insert(block);
        System.out.println(" " + result);
      } catch (RuntimeException e) {
        System.out.println(" failed " + e.getMessage());
        e.fillInStackTrace();
        throw e;
      }
    }

    db.close();
  }

  private static boolean isGenesisFile(File dir, String name) {
    return name.startsWith("genesis") && name.endsWith(".ssz");
  }

  private BeaconChainSpec createBeaconChainSpec(String specConstants) {
    ConfigBuilder<SpecConstantsDataMerged> specConstsBuilder =
        new ConfigBuilder<>(SpecConstantsDataMerged.class);
    specConstsBuilder.addYamlConfig(Paths.get(specConstants).toFile());
    SpecConstantsData constantsData = specConstsBuilder.build();
    SpecHelpersData specHelpersData = new SpecHelpersData();
    SpecData specData = new SpecData();
    specData.setSpecConstants(constantsData);
    specData.setSpecHelpersOptions(specHelpersData);
    return new SpecBuilder().withSpec(specData).buildSpec();
  }

  @NotNull
  private DefaultBeaconChain createBeaconChain(
      BeaconChainSpec spec, BeaconChainStorage chainStorage, ControlledSchedulers schedulers) {
    PerBlockTransition perBlockTransition = StateTransitions.blockTransition(spec);
    EmptySlotTransition emptySlotTransition = StateTransitions.preBlockTransition(spec);
    BeaconBlockVerifier blockVerifier = BeaconBlockVerifier.createDefault(spec);
    BeaconStateVerifier stateVerifier = BeaconStateVerifier.createDefault(spec);

    DefaultBeaconChain beaconChain =
        new DefaultBeaconChain(
            spec,
            emptySlotTransition,
            perBlockTransition,
            blockVerifier,
            stateVerifier,
            chainStorage,
            schedulers);
    beaconChain.init();
    return beaconChain;
  }

  private Optional<File> findStateFile(File initialStateFile, File[] inputFiles) {
    if (initialStateFile != null) {
      return Optional.of(initialStateFile);
    }
    for (File f : inputFiles) {
      if (f.isDirectory()) {
        File[] files = f.listFiles(FeedSyncTool::isGenesisFile);
        if (files.length > 0) {
          return Optional.of(files[0]);
        }
      } else if (isGenesisFile(f.getParentFile(), f.getName())) {
        return Optional.of(f);
      }
    }
    return Optional.empty();
  }

  private void initLogging() {
    LoggerContext context = (LoggerContext) LogManager.getContext(false);
    Configuration config = context.getConfiguration();
    config.getLoggerConfig("root").setLevel(getLogLevel().toLog4j());
    context.updateLoggers();
  }

  @NotNull
  private BytesValue readFile(File file) {
    try {
      return BytesValue.wrap(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      throw new RuntimeException("Cannot load state " + file);
    }
  }
}
