package org.ethereum.beacon.node;

import org.ethereum.beacon.chain.BeaconTupleDetails;
import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BeaconTupleDetailsDumper {
  private final String dir;
  private final Function<BeaconBlock, BytesValue> blockSerializer;
  private final Function<BeaconState, BytesValue> stateSerializer;

  private AtomicInteger currentBlock = new AtomicInteger(0);

  public BeaconTupleDetailsDumper(String dir, SerializerFactory serializerFactory) {
    this.dir = dir;
    this.blockSerializer = serializerFactory.getSerializer(BeaconBlock.class);
    this.stateSerializer = serializerFactory.getSerializer(BeaconState.class);
  }

  private static boolean isDumpFile(Path p) {
    String n = p.getName(p.getNameCount() - 1).toString();
    return (n.startsWith("block_") || n.startsWith("pre_state_") || n.startsWith("post_state_"))
        && n.endsWith(".ssz");
  }

  public void init() throws IOException {
    Path dir = Paths.get(this.dir);
    if (!Files.exists(dir)) {
      Files.createDirectory(dir);
    } else {
      List<Path> files =
          Files.list(dir).filter(BeaconTupleDetailsDumper::isDumpFile).collect(Collectors.toList());
      for (Path p : files) {
        Files.delete(p);
      }
    }
  }

  public void dumpState(String fileName, BeaconState state) throws IOException {
    Files.write(
        Paths.get(dir, fileName + ".ssz"),
        stateSerializer.apply(state).extractArray());
  }

  public void dumpBlock(String fileName, BeaconBlock block) throws IOException {
    Files.write(
        Paths.get(dir, fileName + ".ssz"), blockSerializer.apply(block).extractArray());
  }

  public void dump(BeaconTupleDetails beaconTupleDetails) throws IOException {
    Optional<BeaconStateEx> preState = beaconTupleDetails.getPostSlotState();
    Optional<BeaconStateEx> postState = beaconTupleDetails.getPostBlockState();
    BeaconBlock block = beaconTupleDetails.getBlock();

    if (preState.isPresent() && postState.isPresent()) {
      String num = Integer.toString(currentBlock.incrementAndGet());
      while (num.length() < 4) {
        num = "0" + num;
      }
      dumpBlock("block_" + num, block);
      dumpState("pre_state_" + num, preState.get());
      dumpState("pre_state_" + num, postState.get());
    }
  }
}
