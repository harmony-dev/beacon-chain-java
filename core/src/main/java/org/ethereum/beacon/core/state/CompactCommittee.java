package org.ethereum.beacon.core.state;

import static java.util.Collections.emptyList;

import java.util.List;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Compact committee type.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#compactcommittee">CompactCommittee</a>
 *     in the spec.
 */
@SSZSerializable
public class CompactCommittee {

  public static final CompactCommittee EMPTY = CompactCommittee.create(emptyList(), emptyList());

  @SSZ private final ReadList<Integer, BLSPubkey> pubkeys;
  @SSZ private final ReadList<Integer, UInt64> compactValidators;

  public static CompactCommittee create(List<BLSPubkey> pubkeys, List<UInt64> compactValidators) {
    return new CompactCommittee(
        ReadList.wrap(pubkeys, Integer::new), ReadList.wrap(compactValidators, Integer::new));
  }

  public CompactCommittee(
      ReadList<Integer, BLSPubkey> pubkeys, ReadList<Integer, UInt64> compactValidators) {
    this.pubkeys = pubkeys;
    this.compactValidators = compactValidators;
  }

  public ReadList<Integer, BLSPubkey> getPubkeys() {
    return pubkeys;
  }

  public ReadList<Integer, UInt64> getCompactValidators() {
    return compactValidators;
  }
}
