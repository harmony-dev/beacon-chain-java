package org.ethereum.beacon.wire;

import static org.ethereum.beacon.util.Utils.optionalFlatMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.wire.message.payload.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.payload.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockRootsResponseMessage;
import tech.pegasys.artemis.ethereum.core.Hash32;

/**
 * Asynchronous wire interface for downloading blockchain sync data from remote peer(s)
 */
public interface WireApiSync {
  int MAX_BLOCK_ROOTS_COUNT = 32768;

  /**
   * Requests block roots from remote peer(s)
   */
  CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
      BlockRootsRequestMessage requestMessage);

  /**
   * Requests block headers from remote peer(s)
   */
  CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
      BlockHeadersRequestMessage requestMessage);

  /**
   * Requests block bodies from remote peer(s)
   */
  CompletableFuture<Feedback<BlockBodiesResponseMessage>> requestBlockBodies(
      BlockBodiesRequestMessage requestMessage);

  /**
   * Handy shortcut to download headers+bodies
   */
  default CompletableFuture<Feedback<List<BeaconBlock>>> requestBlocks(
      BlockHeadersRequestMessage requestMessage, ObjectHasher<Hash32> hasher) {

    CompletableFuture<List<BeaconBlockHeader>> headersFuture = requestBlockHeaders(
        requestMessage).thenApply(BlockHeadersResponseMessage::getHeaders);

    CompletableFuture<Feedback<List<BeaconBlockBody>>> bodiesFuture =
        headersFuture.thenCompose(
            headers -> {
              List<Hash32> blockHashes =
                  headers.stream().map(hasher::getHashTruncateLast).collect(Collectors.toList());
              return requestBlockBodies(new BlockBodiesRequestMessage(blockHashes))
                  .thenApply(bb -> bb.map(BlockBodiesResponseMessage::getBlockBodies));
            });

    return headersFuture.thenCombine(
        bodiesFuture,
        (headers, bodies) -> {
          Map<Hash32, BeaconBlockBody> bodyMap =
              bodies.get().stream().collect(Collectors.toMap(hasher::getHash, b -> b, (b1, b2) -> b1));
          return bodies.delegate(
              headers.stream()
                  .map(h -> Optional.ofNullable(bodyMap.get(h.getBlockBodyRoot()))
                              .map(body -> new BeaconBlock(h, body)))
                  .flatMap(optionalFlatMap(b -> b))
                  .collect(Collectors.toList()));
        });
  }
}
