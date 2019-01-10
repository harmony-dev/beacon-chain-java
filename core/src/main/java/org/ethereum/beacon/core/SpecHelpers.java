package org.ethereum.beacon.core;

import org.ethereum.beacon.core.state.ShardCommittee;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public class SpecHelpers {
  private final BeaconChainSpec spec;

  public SpecHelpers(BeaconChainSpec spec) {
    this.spec = spec;
  }

  /*
        earliest_slot_in_array = state.slot - (state.slot % EPOCH_LENGTH) - EPOCH_LENGTH
        assert earliest_slot_in_array <= slot < earliest_slot_in_array + EPOCH_LENGTH * 2
        return state.shard_committees_at_slots[slot - earliest_slot_in_array]
       */
  public ShardCommittee[] get_shard_committees_at_slot(BeaconState state, UInt64 slot) {
    UInt64 earliest_slot_in_array = state.getSlot()
        .minus(state.getSlot().modulo(Epoch.LENGTH))
        .minus(Epoch.LENGTH);
    assertTrue(earliest_slot_in_array.compareTo(slot) <= 0);
    assertTrue(slot.compareTo(earliest_slot_in_array.plus(Epoch.LENGTH * 2)) < 0);
    return state.getShardCommitteesAtSlotsUnsafe()[safeInt(slot.minus(earliest_slot_in_array))];
  }

  /*
    first_committee = get_shard_committees_at_slot(state, slot)[0].committee
    return first_committee[slot % len(first_committee)]
   */
  public UInt24 get_beacon_proposer_index(BeaconState state, UInt64 slot) {
    ShardCommittee[] committees = get_shard_committees_at_slot(state, slot);
    UInt24[] first_committee = committees[0].getCommittee();
    return first_committee[safeInt(slot.modulo(first_committee.length))];
  }

  public static int safeInt(UInt64 uint) {
    long lVal = uint.getValue();
    assertTrue(lVal > 0 && lVal < Integer.MAX_VALUE);
    return (int) lVal;
  }

  private static void assertTrue(boolean assertion) {
    if (!assertion) {
      throw new SpecAssertionFailed();
    }
  }

  public static class SpecAssertionFailed extends RuntimeException {}
}
