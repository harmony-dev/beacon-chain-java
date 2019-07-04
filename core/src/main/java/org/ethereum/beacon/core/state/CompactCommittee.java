package org.ethereum.beacon.core.state;

import static java.util.Collections.emptyList;

import java.util.Collections;
import java.util.List;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Compact committee type.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#compactcommittee">CompactCommittee</a>
 *     in the spec.
 */
public class CompactCommittee {

  public static final CompactCommittee EMPTY = new CompactCommittee(emptyList(), emptyList());

  @SSZ private final List<BLSPubkey> pubkeys;
  @SSZ private final List<UInt64> compactValidators;

  public CompactCommittee(List<BLSPubkey> pubkeys, List<UInt64> compactValidators) {
    this.pubkeys = pubkeys;
    this.compactValidators = compactValidators;
  }

  public List<BLSPubkey> getPubkeys() {
    return pubkeys;
  }

  public List<UInt64> getCompactValidators() {
    return compactValidators;
  }
}
