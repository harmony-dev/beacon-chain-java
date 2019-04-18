package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;
import org.ethereum.beacon.wire.message.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.BlockRootsResponseMessage;

public interface WireApiSync {
  int MAX_BLOCK_ROOTS_COUNT = 32768;

  CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
      BlockRootsRequestMessage requestMessage);

  CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
      BlockHeadersRequestMessage requestMessage);

  CompletableFuture<BlockBodiesResponseMessage> requestBlockBodies(
      BlockBodiesRequestMessage requestMessage);
}
