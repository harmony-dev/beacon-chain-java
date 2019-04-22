package org.ethereum.beacon.wire;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.wire.exceptions.WireInvalidResponseException;
import org.ethereum.beacon.wire.message.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.BlockRootsResponseMessage;
import tech.pegasys.artemis.ethereum.core.Hash32;

public interface WireApiSync {
  int MAX_BLOCK_ROOTS_COUNT = 32768;

  CompletableFuture<BlockRootsResponseMessage> requestBlockRoots(
      BlockRootsRequestMessage requestMessage);

  CompletableFuture<BlockHeadersResponseMessage> requestBlockHeaders(
      BlockHeadersRequestMessage requestMessage);

  CompletableFuture<BlockBodiesResponseMessage> requestBlockBodies(
      BlockBodiesRequestMessage requestMessage);

  default CompletableFuture<List<BeaconBlock>> requestBlocks(
      BlockHeadersRequestMessage requestMessage, ObjectHasher<Hash32> hasher) {

    CompletableFuture<List<BeaconBlockHeader>> headersFuture = requestBlockHeaders(
        requestMessage).thenApply(BlockHeadersResponseMessage::getHeaders);

    CompletableFuture<List<BeaconBlockBody>> bodiesFuture = headersFuture
        .thenCompose(headers -> {
          List<Hash32> blockHashes = headers.stream()
              .map(BeaconBlockHeader::getBlockBodyRoot)
              .collect(Collectors.toList());
          return requestBlockBodies(new BlockBodiesRequestMessage(blockHashes))
              .thenApply(BlockBodiesResponseMessage::getBlockBodies);
        });

    return headersFuture.thenCombine(bodiesFuture,
        (headers, bodies) -> {
          Map<Hash32, BeaconBlockBody> bodyMap =
              bodies.stream().collect(Collectors.toMap(hasher::getHash, b -> b));
          return headers
              .stream()
              .map(
                  h -> {
                    BeaconBlockBody body = bodyMap.get(h.getBlockBodyRoot());
                    if (body != null) {
                      return new BeaconBlock(h, body);
                    } else {
                      return null;
                    }
                  })
              .collect(Collectors.toList());
        });
  }

}
