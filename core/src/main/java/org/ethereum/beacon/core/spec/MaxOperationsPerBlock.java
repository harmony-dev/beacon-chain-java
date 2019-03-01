package org.ethereum.beacon.core.spec;

/**
 * Constants limiting number of each beacon chain operation that a block can hold.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/core/0_beacon-chain.md#max-operations-per-block">Max
 *     operations per block</a> in the spec.
 */
public interface MaxOperationsPerBlock {

  int MAX_PROPOSER_SLASHINGS = 1 << 4; // 16
  int MAX_ATTESTER_SLASHINGS = 1;
  int MAX_ATTESTATIONS = 1 << 7; // 128
  int MAX_DEPOSITS = 1 << 4; // 16
  int MAX_VOLUNTARY_EXITS = 1 << 4; // 16
  int MAX_TRANSFERS = 1 << 4; // 16

  /* Values defined in the spec. */

  default int getMaxProposerSlashings() {
    return MAX_PROPOSER_SLASHINGS;
  }

  default int getMaxAttesterSlashings() {
    return MAX_ATTESTER_SLASHINGS;
  }


  default int getMaxAttestations() {
    return MAX_ATTESTATIONS;
  }

  default int getMaxDeposits() {
    return MAX_DEPOSITS;
  }

  default int getMaxVoluntaryExits() {
    return MAX_VOLUNTARY_EXITS;
  }

  default int getMaxTransfers() {
    return MAX_TRANSFERS;
  }
}
