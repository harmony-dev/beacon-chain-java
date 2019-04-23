package org.ethereum.beacon.wire.sync;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.wire.Feedback;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public interface SyncQueue {

  interface BlockRequest {

    Optional<SlotNumber> getStartSlot();

    Optional<Hash32> getStartRoot();

    UInt64 getMaxCount();

    boolean isReverse();

    UInt64 getStep();
  }

  Publisher<BlockRequest> getBlockRequestsStream();

  Publisher<Feedback<BeaconBlock>> getBlocksStream();

  void subscribeToFinalBlocks(Publisher<Hash32> finalBlockRootStream);

  void subscribeToNewBlocks(Publisher<Feedback<BeaconBlock>> blocksStream);
}
