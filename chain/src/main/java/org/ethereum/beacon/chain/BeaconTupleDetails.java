package org.ethereum.beacon.chain;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlock;

public class BeaconTupleDetails extends BeaconTuple {

  private final BeaconStateEx postSlotState;
  private final BeaconStateEx postBlockState;

  public BeaconTupleDetails(
      @Nonnull BeaconBlock block,
      @Nullable BeaconStateEx postSlotState,
      @Nullable BeaconStateEx postBlockState,
      @Nonnull BeaconStateEx finalState) {

    super(block, finalState);
    this.postSlotState = postSlotState;
    this.postBlockState = postBlockState;
  }
  public BeaconTupleDetails(BeaconTuple tuple) {
    this(tuple.getBlock(), null, null, tuple.getState());
  }

  public Optional<BeaconStateEx> getPostSlotState() {
    return Optional.ofNullable(postSlotState);
  }

  public Optional<BeaconStateEx> getPostBlockState() {
    return Optional.ofNullable(postBlockState);
  }

  public BeaconStateEx getFinalState() {
    return getState();
  }

}
