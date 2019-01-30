package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.core.types.Gwei;
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
  Gwei MIN_DEPOSIT = Gwei.ofEthers(1); // 1 ETH
  Gwei MAX_DEPOSIT = Gwei.ofEthers(1 << 5); // 32 ETH

  /* Values defined in the spec. */

  Address getDepositContractAddress();

  UInt64 getDepositContractTreeDepth();

  Gwei getMinDeposit();

  Gwei getMaxDeposit();
}
