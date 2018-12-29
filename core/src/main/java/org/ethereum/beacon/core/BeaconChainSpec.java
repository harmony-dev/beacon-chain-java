package org.ethereum.beacon.core;

import tech.pegasys.artemis.util.uint.UInt64;

/** Various constants related to beacon chain parameters. */
public class BeaconChainSpec {

  /** Shard number for the Beacon chain itself. */
  public static final UInt64 SHARD = UInt64.MAX_VALUE;

  /** Total number of shards. */
  public static final int SHARD_COUNT = 1 << 10; // 1024
}
