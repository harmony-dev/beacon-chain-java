package org.ethereum.beacon.core.state;

import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.ValidatorIndex;
import java.util.List;

/** Validator committee assigned to a certain shard. */
public class ShardCommittee {

  /** Validator indices. */
  private final List<ValidatorIndex> committee;
  /** Committee index. */
  private final CommitteeIndex index;

  public ShardCommittee(List<ValidatorIndex> committee, CommitteeIndex index) {
    this.committee = committee;
    this.index = index;
  }

  public List<ValidatorIndex> getCommittee() {
    return committee;
  }

  public CommitteeIndex getIndex() {
    return index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ShardCommittee committee1 = (ShardCommittee) o;

    if (!committee.equals(committee1.committee)) {
      return false;
    }
    return index.equals(committee1.index);
  }

  @Override
  public int hashCode() {
    int result = committee.hashCode();
    result = 31 * result + index.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ShardCommittee[" + index + ": " + committee + "]";
  }
}
