package org.ethereum.beacon.core.spec;

import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Flags for validator registry delta calculation.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#validator-registry-delta-flags">Validator
 *     registry delta flags</a> in the spec.
 */
public interface ValidatorRegistryDeltaFlags {

  UInt64 ACTIVATION = UInt64.valueOf(0);

  UInt64 EXIT = UInt64.valueOf(1);
}
