package org.ethereum.beacon.core;

import tech.pegasys.artemis.util.uint.UInt64;

/** Various constants related to beacon chain parameters. */
public abstract class BeaconChainSpec {
  private BeaconChainSpec() {}

  /** Shard number for the Beacon chain itself. */
  public static final UInt64 SHARD = UInt64.MAX_VALUE;

  /** Total number of shards. */
  public static final int SHARD_COUNT = 1 << 10; // 1024

  /** Max slot number. */
  public static final UInt64 FAR_FUTURE_SLOT = UInt64.MAX_VALUE;

  public static final int TARGET_COMMITTEE_SIZE = 128;

  /** Genesis parameters. */
  public abstract static class Genesis {
    private Genesis() {}

    /** Initial slot number. */
    public static final UInt64 SLOT = UInt64.ZERO;
    /** Initial fork version. */
    public static final UInt64 FORK_VERSION = UInt64.ZERO;
    /** Initial start shard. */
    public static final UInt64 START_SHARD = UInt64.ZERO;
  }
}
