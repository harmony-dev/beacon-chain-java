package org.ethereum.beacon.core.spec;

import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Validator status flags.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#status-flags">Status
 *     flags</a> in the spec.
 */
public interface ValidatorStatusFlags {

  UInt64 INITIATED_EXIT = UInt64.valueOf(1);

  UInt64 WITHDRAWABLE = UInt64.valueOf(2);
}
