package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;
import java.util.List;

/** Validator committee assigned to a certain shard. */
public class ShardCommittee {

  /** Validator indices. */
  private final List<ValidatorIndex> committee;
  /** Shard number. */
  private final ShardNumber shard;

  public ShardCommittee(List<ValidatorIndex> committee, ShardNumber shard) {
    this.committee = committee;
    this.shard = shard;
  }

  public List<ValidatorIndex> getCommittee() {
    return committee;
  }

  public ShardNumber getShard() {
    return shard;
  }
}
