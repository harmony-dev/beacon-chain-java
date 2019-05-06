package org.ethereum.beacon.wire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.exceptions.WireIllegalArgumentsException;
import org.ethereum.beacon.wire.message.payload.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage.BlockRootSlot;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class WireApiSyncServer implements WireApiSync {

  private final BeaconChainStorage storage;

  public WireApiSyncServer(BeaconChainStorage storage) {
    this.storage = storage;
  }

  @Override
  public CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
      BlockRootsRequestMessage requestMessage) {
    CompletableFuture<BlockRootsResponseMessage> ret = new CompletableFuture();
    if (requestMessage.getCount().compareTo(UInt64.valueOf(MAX_BLOCK_ROOTS_COUNT)) > 0) {
      ret.completeExceptionally(new WireIllegalArgumentsException(
              "Too many block roots requested: " + requestMessage.getCount()));
    } else {
      List<BlockRootSlot> roots = new ArrayList<>();
      for (SlotNumber slot : requestMessage.getStartSlot().iterateTo(
          requestMessage.getStartSlot().plus(requestMessage.getCount()))) {
        List<Hash32> slotRoots = storage.getBlockStorage().getSlotBlocks(slot);
        for (Hash32 slotRoot : slotRoots) {
          roots.add(new BlockRootSlot(slotRoot, slot));
        }
      }
      ret.complete(new BlockRootsResponseMessage(roots));
    }
    return ret;
  }

  @Override
  public CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
      BlockHeadersRequestMessage requestMessage) {
    CompletableFuture<BlockHeadersResponseMessage> ret = new CompletableFuture<>();

    SlotNumber slot;
    if (!BlockHeadersRequestMessage.NULL_START_SLOT.equals(requestMessage.getStartSlot())) {
      slot = requestMessage.getStartSlot();
    } else {
      Optional<BeaconBlock> blockOpt = storage.getBlockStorage().get(requestMessage.getStartRoot());
      slot = blockOpt.map(BeaconBlock::getSlot).orElse(null);
    }

    if (slot != null) {
      List<BeaconBlockHeader> headers = new ArrayList<>();
      int increment = requestMessage.getSkipSlots().getIntValue() + 1;
      SlotNumber maxSlot = storage.getBlockStorage().getMaxSlot();
      SlotNumber prevSlot = SlotNumber.ZERO;
      for(int i = 0; i < requestMessage.getMaxHeaders().intValue(); i++) {

        List<Hash32> slotBlocks = Collections.emptyList();
        SlotNumber nonEmptySlot = slot;
        while (slotBlocks.isEmpty() && nonEmptySlot.greater(prevSlot)) {
          slotBlocks = storage.getBlockStorage().getSlotBlocks(nonEmptySlot);
          nonEmptySlot = nonEmptySlot.decrement();
        }
        headers.add(storage.getBlockHeaderStorage().get(slotBlocks.get(0)).get());
        slot = slot.plus(increment);
        if (slot.greater(maxSlot)) {
          break;
        }
        prevSlot = nonEmptySlot;
      }
      ret.complete(new BlockHeadersResponseMessage(requestMessage, headers));
    } else {
      ret.complete(new BlockHeadersResponseMessage(requestMessage, Collections.emptyList()));
    }
    return ret;
  }



  @Override
  public CompletableFuture<Feedback<BlockBodiesResponseMessage>> requestBlockBodies(
      BlockBodiesRequestMessage requestMessage) {

    List<BeaconBlockBody> bodyList = requestMessage.getBlockTreeRoots().stream()
        .map(blockRoot -> storage.getBlockStorage().get(blockRoot))
        .flatMap(opt -> opt.isPresent() ? Stream.of(opt.map(BeaconBlock::getBody).get()) : Stream.empty())
        .collect(Collectors.toList());
    return CompletableFuture.completedFuture(
        Feedback.of(new BlockBodiesResponseMessage(requestMessage, bodyList)));
  }
}
