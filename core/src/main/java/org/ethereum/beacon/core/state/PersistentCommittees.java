package org.ethereum.beacon.core.state;

import tech.pegasys.artemis.util.uint.UInt24;

/** Various constants and utility methods to work with persistent committees. */
public abstract class PersistentCommittees {
  private PersistentCommittees() {}

  /** A list with no committees assigned to any shard at any slot. */
  public static final UInt24[][] EMPTY = new UInt24[0][];
}
