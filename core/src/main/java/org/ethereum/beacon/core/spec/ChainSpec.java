package org.ethereum.beacon.core.spec;

import org.ethereum.beacon.types.Ether;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public interface ChainSpec extends InitialValues, MiscParameters, DepositContractParameters {

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

        @Override
        public Address getDepositContractAddress() {
          return DEPOSIT_CONTRACT_ADDRESS;
        }

        @Override
        public UInt64 getDepositContractTreeDepth() {
          return DEPOSIT_CONTRACT_TREE_DEPTH;
        }

        @Override
        public Ether getMinDeposit() {
          return MIN_DEPOSIT;
        }

        @Override
        public Ether getMaxDeposit() {
          return MAX_DEPOSIT;
        }

        @Override
        public UInt64 getGenesisForkVersion() {
          return GENESIS_FORK_VERSION;
        }

        @Override
        public UInt64 getGenesisSlot() {
          return GENESIS_SLOT;
        }

        @Override
        public UInt64 getGenesisStartShard() {
          return GENESIS_START_SHARD;
        }

        @Override
        public UInt64 getFarFutureSlot() {
          return FAR_FUTURE_SLOT;
        }

        @Override
        public Bytes96 getEmptySignature() {
          return EMPTY_SIGNATURE;
        }

        @Override
        public Bytes1 getBlsWithdrawalPrefixByte() {
          return BLS_WITHDRAWAL_PREFIX_BYTE;
        }
      };
}
