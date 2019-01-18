package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.types.Ether;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Deposit contract constants.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#deposit-contract">Deposit
 *     contract</a> in the spec
 */
public interface DepositContractParameters {

  Address DEPOSIT_CONTRACT_ADDRESS =
      Address.fromHexString("0x0000000000000000000000000000000000000000"); // TBD
  UInt64 DEPOSIT_CONTRACT_TREE_DEPTH = UInt64.valueOf(1 << 5); // 32
  Ether MIN_DEPOSIT = Ether.valueOf(1); // 1 ETH
  Ether MAX_DEPOSIT = Ether.valueOf(1 << 5); // 32 ETH

  /* Values defined in the spec. */

  Address getDepositContractAddress();

  UInt64 getDepositContractTreeDepth();

  Ether getMinDeposit();

  Ether getMaxDeposit();
}
