package org.ethereum.beacon.emulator.config.chainspec;

import org.ethereum.beacon.core.spec.SpecConstants;

public interface SpecConstantsData {
  static SpecConstantsData getDefaultCopy() {
    SpecConstants specs = SpecConstants.DEFAULT;

    return new SpecConstantsData() {
      private DepositContractParametersData depositContractParametersData =
          new DepositContractParametersData() {
            {
              setDEPOSIT_CONTRACT_ADDRESS(specs.getDepositContractAddress().toString());
              setDEPOSIT_CONTRACT_TREE_DEPTH(specs.getDepositContractTreeDepth().toString());
            }
          };

      private HonestValidatorParametersData honestValidatorParametersData =
          new HonestValidatorParametersData() {
            {
              setETH1_FOLLOW_DISTANCE(specs.getEth1FollowDistance());
            }
          };

      private InitialValuesData initialValuesData =
          new InitialValuesData() {
            {
              setBLS_WITHDRAWAL_PREFIX_BYTE(specs.getBlsWithdrawalPrefixByte().toString());
              setEMPTY_SIGNATURE(specs.getEmptySignature().copy().toString());
              setFAR_FUTURE_EPOCH(specs.getFarFutureEpoch().toString());
              setGENESIS_FORK_VERSION(specs.getGenesisForkVersion().toString());
              setGENESIS_SLOT(Long.toUnsignedString(specs.getGenesisSlot().getValue()));
              setGENESIS_START_SHARD(specs.getGenesisStartShard().intValue());
              setZERO_HASH(specs.getZeroHash().toString());
            }
          };

      private MaxOperationsPerBlockData maxOperationsPerBlockData =
          new MaxOperationsPerBlockData() {
            {
              setMAX_ATTESTATIONS(specs.getMaxAttestations());
              setMAX_ATTESTER_SLASHINGS(specs.getMaxAttesterSlashings());
              setMAX_DEPOSITS(specs.getMaxDeposits());
              setMAX_PROPOSER_SLASHINGS(specs.getMaxProposerSlashings());
              setMAX_TRANSFERS(specs.getMaxTransfers());
              setMAX_VOLUNTARY_EXITS(specs.getMaxVoluntaryExits());
            }
          };

      private MiscParametersData miscParametersData =
          new MiscParametersData() {
            {
              setBEACON_CHAIN_SHARD_NUMBER(specs.getBeaconChainShardNumber().toString());
              setMAX_BALANCE_CHURN_QUOTIENT(specs.getMaxBalanceChurnQuotient().toString());
              setMAX_INDICES_PER_SLASHABLE_VOTE(specs.getMaxIndicesPerSlashableVote().toString());
              setSHARD_COUNT(specs.getShardCount().toString());
              setTARGET_COMMITTEE_SIZE(specs.getTargetCommitteeSize().toString());
              setMAX_EXIT_DEQUEUES_PER_EPOCH(specs.getMaxExitDequesPerEpoch().toString());
            }
          };

      private GweiValuesData gweiValuesData =
          new GweiValuesData() {
            {
              setEJECTION_BALANCE(Long.toUnsignedString(specs.getEjectionBalance().getValue()));
              setFORK_CHOICE_BALANCE_INCREMENT(
                  Long.toUnsignedString(specs.getForkChoiceBalanceIncrement().getValue()));
              setMIN_DEPOSIT_AMOUNT(Long.toUnsignedString(specs.getMinDepositAmount().getValue()));
              setMAX_DEPOSIT_AMOUNT(Long.toUnsignedString(specs.getMaxDepositAmount().getValue()));
            }
          };

      private RewardAndPenaltyQuotientsData rewardAndPenaltyQuotientsData =
          new RewardAndPenaltyQuotientsData() {
            {
              setBASE_REWARD_QUOTIENT(specs.getBaseRewardQuotient().toString());
              setINACTIVITY_PENALTY_QUOTIENT(specs.getInactivityPenaltyQuotient().toString());
              setWHISTLEBLOWER_REWARD_QUOTIENT(specs.getWhistleblowerRewardQuotient().toString());
              setATTESTATION_INCLUSION_REWARD_QUOTIENT(
                  specs.getAttestationInclusionRewardQuotient().toString());
              setMIN_PENALTY_QUOTIENT(specs.getMinPenaltyQuotient().toString());
            }
          };

      private StateListLengthsData stateListLengthsData =
          new StateListLengthsData() {
            {
              setLATEST_RANDAO_MIXES_LENGTH(specs.getLatestRandaoMixesLength().toString());
              setLATEST_ACTIVE_INDEX_ROOTS_LENGTH(
                  specs.getLatestActiveIndexRootsLength().toString());
              setLATEST_SLASHED_EXIT_LENGTH(specs.getLatestSlashedExitLength().toString());
            }
          };

      private TimeParametersData timeParametersData =
          new TimeParametersData() {
            {
              setMIN_ATTESTATION_INCLUSION_DELAY(
                  Long.toUnsignedString(specs.getMinAttestationInclusionDelay().getValue()));
              setACTIVATION_EXIT_DELAY(specs.getActivationExitDelay().toString());
              setEPOCHS_PER_ETH1_VOTING_PERIOD(specs.getEth1DataVotingPeriod().toString());
              setMIN_SEED_LOOKAHEAD(specs.getMinSeedLookahead().toString());
              setMIN_VALIDATOR_WITHDRAWABILITY_DELAY(
                  specs.getMinValidatorWithdrawabilityDelay().toString());
              setSECONDS_PER_SLOT(Long.toString(specs.getSecondsPerSlot().getValue()));
              setSLOTS_PER_EPOCH(Long.toUnsignedString(specs.getSlotsPerEpoch().getValue()));
              setPERSISTENT_COMMITTEE_PERIOD(specs.getPersistentCommitteePeriod().toString());
              setSLOTS_PER_HISTORICAL_ROOT(
                  Long.toUnsignedString(specs.getSlotsPerHistoricalRoot().getValue()));
            }
          };

      @Override
      public DepositContractParametersData getDepositContractParameters() {
        return depositContractParametersData;
      }

      @Override
      public void setDepositContractParameters(
          DepositContractParametersData depositContractParameters) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public HonestValidatorParametersData getHonestValidatorParameters() {
        return honestValidatorParametersData;
      }

      @Override
      public void setHonestValidatorParameters(
          HonestValidatorParametersData honestValidatorParameters) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public InitialValuesData getInitialValues() {
        return initialValuesData;
      }

      @Override
      public void setInitialValues(InitialValuesData initialValues) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public MaxOperationsPerBlockData getMaxOperationsPerBlock() {
        return maxOperationsPerBlockData;
      }

      @Override
      public void setMaxOperationsPerBlock(MaxOperationsPerBlockData maxOperationsPerBlock) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public MiscParametersData getMiscParameters() {
        return miscParametersData;
      }

      @Override
      public void setMiscParameters(MiscParametersData miscParameters) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public GweiValuesData getGweiValues() {
        return gweiValuesData;
      }

      @Override
      public void setGweiValues(GweiValuesData gweiValues) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public RewardAndPenaltyQuotientsData getRewardAndPenaltyQuotients() {
        return rewardAndPenaltyQuotientsData;
      }

      @Override
      public void setRewardAndPenaltyQuotients(
          RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public StateListLengthsData getStateListLengths() {
        return stateListLengthsData;
      }

      @Override
      public void setStateListLengths(StateListLengthsData stateListLengths) {
        throw new RuntimeException("Not supported!");
      }

      @Override
      public TimeParametersData getTimeParameters() {
        return timeParametersData;
      }

      @Override
      public void setTimeParameters(TimeParametersData timeParameters) {
        throw new RuntimeException("Not supported!");
      }
    };
  }

  DepositContractParametersData getDepositContractParameters();

  void setDepositContractParameters(DepositContractParametersData depositContractParameters);

  HonestValidatorParametersData getHonestValidatorParameters();

  void setHonestValidatorParameters(HonestValidatorParametersData honestValidatorParameters);

  InitialValuesData getInitialValues();

  void setInitialValues(InitialValuesData initialValues);

  MaxOperationsPerBlockData getMaxOperationsPerBlock();

  void setMaxOperationsPerBlock(MaxOperationsPerBlockData maxOperationsPerBlock);

  MiscParametersData getMiscParameters();

  void setMiscParameters(MiscParametersData miscParameters);

  GweiValuesData getGweiValues();

  void setGweiValues(GweiValuesData gweiValues);

  RewardAndPenaltyQuotientsData getRewardAndPenaltyQuotients();

  void setRewardAndPenaltyQuotients(RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients);

  StateListLengthsData getStateListLengths();

  void setStateListLengths(StateListLengthsData stateListLengths);

  TimeParametersData getTimeParameters();

  void setTimeParameters(TimeParametersData timeParameters);
}
