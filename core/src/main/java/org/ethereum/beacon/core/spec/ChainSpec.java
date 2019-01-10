package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.types.Ether;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public interface ChainSpec extends MiscParameters {

  ChainSpec DEFAULT =
      new ChainSpec() {
        @Override
        public UInt64 getShardCount() {
          return SHARD_COUNT;
        }

        @Override
        public UInt24 getTargetCommitteeSize() {
          return TARGET_COMMITTEE_SIZE;
        }

        @Override
        public Ether getEjectionBalance() {
          return EJECTION_BALANCE;
        }

        @Override
        public UInt64 getMaxBalanceChurnQuotient() {
          return MAX_BALANCE_CHURN_QUOTIENT;
        }

        @Override
        public UInt64 getBeaconChainShardNumber() {
          return BEACON_CHAIN_SHARD_NUMBER;
        }

        @Override
        public UInt64 getMaxCasperVotes() {
          return MAX_CASPER_VOTES;
        }

        @Override
        public UInt64 getLatestBlockRootsLength() {
          return LATEST_BLOCK_ROOTS_LENGTH;
        }

        @Override
        public UInt64 getLatestRandaoMixesLength() {
          return LATEST_RANDAO_MIXES_LENGTH;
        }

        @Override
        public UInt64 getLatestPenalizedExitLength() {
          return LATEST_PENALIZED_EXIT_LENGTH;
        }
      };
}
