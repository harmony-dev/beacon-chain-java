package org.ethereum.beacon.wire;

import java.util.concurrent.Future;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.wire.message.BlockBodiesRequestMessage;
import org.ethereum.beacon.wire.message.BlockBodiesResponseMessage;
import org.ethereum.beacon.wire.message.BlockRootsRequestMessage;
import org.ethereum.beacon.wire.message.BlockHeadersResponseMessage;
import org.ethereum.beacon.wire.message.BlockHeadersRequestMessage;
import org.ethereum.beacon.wire.message.BlockRootsResponseMessage;
import org.reactivestreams.Publisher;

public interface WireApi {

  void sendProposedBlock(BeaconBlock block);

  void sendAttestation(Attestation attestation);

  Publisher<BeaconBlock> inboundBlocksStream();

  Publisher<Attestation> inboundAttestationsStream();

  Future<BlockRootsResponseMessage> requestBlockRoots(BlockHeadersRequestMessage requestMessage);

  Future<BlockHeadersResponseMessage> requestBlockHeaders(BlockRootsRequestMessage requestMessage);

  Future<BlockBodiesResponseMessage> requestBlockBodies(BlockBodiesRequestMessage requestMessage);
}
