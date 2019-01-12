package org.ethereum.beacon.core.state;

/** Various constants and utility methods to work with {@link ShardCommittee}. */
public abstract class ShardCommittees {
  private ShardCommittees() {}

  /** A list without any shard committees assigned to any slot. */
  public static final ShardCommittee[][] EMPTY = new ShardCommittee[0][];
}
