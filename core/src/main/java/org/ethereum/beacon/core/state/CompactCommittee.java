package org.ethereum.beacon.core.state;

import java.util.List;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;

/**
 * Compact committee type.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.8.0/specs/core/0_beacon-chain.md#compactcommittee">CompactCommittee</a>
 *     in the spec.
 */
public class CompactCommittee {

  @SSZ private final List<BLSPubkey> pubkeys;
  @SSZ private final List<ValidatorIndex> compactValidators;

  public CompactCommittee(List<BLSPubkey> pubkeys, List<ValidatorIndex> compactValidators) {
    this.pubkeys = pubkeys;
    this.compactValidators = compactValidators;
  }

  public List<BLSPubkey> getPubkeys() {
    return pubkeys;
  }

  public List<ValidatorIndex> getCompactValidators() {
    return compactValidators;
  }
}
