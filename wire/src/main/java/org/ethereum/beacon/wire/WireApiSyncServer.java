package org.ethereum.beacon.wire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.exceptions.WireIllegalArgumentsException;
import org.ethereum.beacon.wire.message.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.BlockRootsResponseMessage;
import org.ethereum.beacon.wire.message.BlockRootsResponseMessage.BlockRootSlot;
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
      ret.complete(new BlockRootsResponseMessage(requestMessage, roots));
    }
    return ret;
  }

  @Override
  public CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
      BlockHeadersRequestMessage requestMessage) {
    CompletableFuture<BlockHeadersResponseMessage> ret = new CompletableFuture<>();
    Optional<BeaconBlock> blockOpt = storage.getBlockStorage()
        .get(requestMessage.getStartRoot());
    if (blockOpt.isPresent()) {
      BeaconBlock block = blockOpt.get();
      if (!block.getSlot().equals(requestMessage.getStartSlot())) {
        ret.completeExceptionally(
            new WireIllegalArgumentsException("Requested start slot doesn't match block root: "
                    + requestMessage.getStartRoot() + ", " + requestMessage.getStartSlot()));
      } else {
        List<BeaconBlockHeader> headers = new ArrayList<>();
        int increment = requestMessage.getSkipSlots().getIntValue() + 1;
        SlotNumber maxSlot = storage.getBlockStorage().getMaxSlot();
        SlotNumber slot = requestMessage.getStartSlot();
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
      }
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
        .map(opt -> opt.map(BeaconBlock::getBody).orElse(BeaconBlockBody.EMPTY))
        .collect(Collectors.toList());
    return CompletableFuture.completedFuture(
        Feedback.of(new BlockBodiesResponseMessage(requestMessage, bodyList)));
  }
}
