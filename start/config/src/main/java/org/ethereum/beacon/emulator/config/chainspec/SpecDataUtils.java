package org.ethereum.beacon.emulator.config.chainspec;

import org.ethereum.beacon.core.spec.SpecConstants;

/** Various utility methods for different spec data classes. */
public abstract class SpecDataUtils {
  private SpecDataUtils() {}

  public static SpecConstantsData createSpecConstantsData(SpecConstants constants) {
    SpecConstantsData specConstantsData = new SpecConstantsData();
    DepositContractParametersData depositContractParameters =
        new DepositContractParametersData() {
          {
            setDEPOSIT_CONTRACT_ADDRESS(constants.getDepositContractAddress().toString());
            setDEPOSIT_CONTRACT_TREE_DEPTH(constants.getDepositContractTreeDepth().toString());
          }
        };

    HonestValidatorParametersData honestValidatorParameters =
        new HonestValidatorParametersData() {
          {
            setETH1_FOLLOW_DISTANCE(constants.getEth1FollowDistance());
          }
        };

    InitialValuesData initialValues =
        new InitialValuesData() {
          {
            setBLS_WITHDRAWAL_PREFIX_BYTE(constants.getBlsWithdrawalPrefixByte().toString());
            setEMPTY_SIGNATURE(constants.getEmptySignature().copy().toString());
            setFAR_FUTURE_EPOCH(constants.getFarFutureEpoch().toString());
            setGENESIS_FORK_VERSION(constants.getGenesisForkVersion().toString());
            setGENESIS_SLOT(Long.toUnsignedString(constants.getGenesisSlot().getValue()));
            setGENESIS_START_SHARD(constants.getGenesisStartShard().intValue());
            setZERO_HASH(constants.getZeroHash().toString());
          }
        };

    MaxOperationsPerBlockData maxOperationsPerBlock =
        new MaxOperationsPerBlockData() {
          {
            setMAX_ATTESTATIONS(constants.getMaxAttestations());
            setMAX_ATTESTER_SLASHINGS(constants.getMaxAttesterSlashings());
            setMAX_DEPOSITS(constants.getMaxDeposits());
            setMAX_PROPOSER_SLASHINGS(constants.getMaxProposerSlashings());
            setMAX_TRANSFERS(constants.getMaxTransfers());
            setMAX_VOLUNTARY_EXITS(constants.getMaxVoluntaryExits());
          }
        };

    MiscParametersData miscParameters =
        new MiscParametersData() {
          {
            setBEACON_CHAIN_SHARD_NUMBER(constants.getBeaconChainShardNumber().toString());
            setMAX_BALANCE_CHURN_QUOTIENT(constants.getMaxBalanceChurnQuotient().toString());
            setMAX_INDICES_PER_SLASHABLE_VOTE(constants.getMaxIndicesPerSlashableVote().toString());
            setSHARD_COUNT(constants.getShardCount().toString());
            setTARGET_COMMITTEE_SIZE(constants.getTargetCommitteeSize().toString());
            setMAX_EXIT_DEQUEUES_PER_EPOCH(constants.getMaxExitDequesPerEpoch().toString());
          }
        };

    GweiValuesData gweiValues =
        new GweiValuesData() {
          {
            setEJECTION_BALANCE(Long.toUnsignedString(constants.getEjectionBalance().getValue()));
            setFORK_CHOICE_BALANCE_INCREMENT(
                Long.toUnsignedString(constants.getForkChoiceBalanceIncrement().getValue()));
            setMIN_DEPOSIT_AMOUNT(
                Long.toUnsignedString(constants.getMinDepositAmount().getValue()));
            setMAX_DEPOSIT_AMOUNT(
                Long.toUnsignedString(constants.getMaxDepositAmount().getValue()));
          }
        };

    RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients =
        new RewardAndPenaltyQuotientsData() {
          {
            setBASE_REWARD_QUOTIENT(constants.getBaseRewardQuotient().toString());
            setINACTIVITY_PENALTY_QUOTIENT(constants.getInactivityPenaltyQuotient().toString());
            setWHISTLEBLOWER_REWARD_QUOTIENT(constants.getWhistleblowerRewardQuotient().toString());
            setATTESTATION_INCLUSION_REWARD_QUOTIENT(
                constants.getAttestationInclusionRewardQuotient().toString());
            setMIN_PENALTY_QUOTIENT(constants.getMinPenaltyQuotient().toString());
          }
        };

    StateListLengthsData stateListLengths =
        new StateListLengthsData() {
          {
            setLATEST_RANDAO_MIXES_LENGTH(constants.getLatestRandaoMixesLength().toString());
            setLATEST_ACTIVE_INDEX_ROOTS_LENGTH(
                constants.getLatestActiveIndexRootsLength().toString());
            setLATEST_SLASHED_EXIT_LENGTH(constants.getLatestSlashedExitLength().toString());
          }
        };

    TimeParametersData timeParameters =
        new TimeParametersData() {
          {
            setMIN_ATTESTATION_INCLUSION_DELAY(
                Long.toUnsignedString(constants.getMinAttestationInclusionDelay().getValue()));
            setACTIVATION_EXIT_DELAY(constants.getActivationExitDelay().toString());
            setEPOCHS_PER_ETH1_VOTING_PERIOD(constants.getEpochsPerEth1VotingPeriod().toString());
            setMIN_SEED_LOOKAHEAD(constants.getMinSeedLookahead().toString());
            setMIN_VALIDATOR_WITHDRAWABILITY_DELAY(
                constants.getMinValidatorWithdrawabilityDelay().toString());
            setSECONDS_PER_SLOT(Long.toString(constants.getSecondsPerSlot().getValue()));
            setSLOTS_PER_EPOCH(Long.toUnsignedString(constants.getSlotsPerEpoch().getValue()));
            setPERSISTENT_COMMITTEE_PERIOD(constants.getPersistentCommitteePeriod().toString());
            setSLOTS_PER_HISTORICAL_ROOT(
                Long.toUnsignedString(constants.getSlotsPerHistoricalRoot().getValue()));
          }
        };

    specConstantsData.setDepositContractParameters(depositContractParameters);
    specConstantsData.setGweiValues(gweiValues);
    specConstantsData.setHonestValidatorParameters(honestValidatorParameters);
    specConstantsData.setInitialValues(initialValues);
    specConstantsData.setMaxOperationsPerBlock(maxOperationsPerBlock);
    specConstantsData.setMiscParameters(miscParameters);
    specConstantsData.setRewardAndPenaltyQuotients(rewardAndPenaltyQuotients);
    specConstantsData.setStateListLengths(stateListLengths);
    specConstantsData.setTimeParameters(timeParameters);

    return specConstantsData;
  }
}
