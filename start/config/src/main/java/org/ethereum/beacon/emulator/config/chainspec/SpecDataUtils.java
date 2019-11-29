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
            setBLS_WITHDRAWAL_PREFIX(constants.getBlsWithdrawalPrefix().toString());
            setGENESIS_SLOT(Long.toUnsignedString(constants.getGenesisSlot().getValue()));
          }
        };

    MaxOperationsPerBlockData maxOperationsPerBlock =
        new MaxOperationsPerBlockData() {
          {
            setMAX_ATTESTATIONS(constants.getMaxAttestations());
            setMAX_ATTESTER_SLASHINGS(constants.getMaxAttesterSlashings());
            setMAX_DEPOSITS(constants.getMaxDeposits());
            setMAX_PROPOSER_SLASHINGS(constants.getMaxProposerSlashings());
            setMAX_VOLUNTARY_EXITS(constants.getMaxVoluntaryExits());
          }
        };

    MiscParametersData miscParameters =
        new MiscParametersData() {
          {
            setMAX_VALIDATORS_PER_COMMITTEE(constants.getMaxValidatorsPerCommittee().toString());
            setMIN_PER_EPOCH_CHURN_LIMIT(constants.getMinPerEpochChurnLimit().toString());
            setCHURN_LIMIT_QUOTIENT(constants.getChurnLimitQuotient().toString());
            setMAX_COMMITTEES_PER_SLOT(constants.getMaxCommitteesPerSlot().toString());
            setTARGET_COMMITTEE_SIZE(constants.getTargetCommitteeSize().toString());
            setSHUFFLE_ROUND_COUNT(constants.getShuffleRoundCount());
            setMIN_GENESIS_ACTIVE_VALIDATOR_COUNT(constants.getMinGenesisActiveValidatorCount().toString());
            setMIN_GENESIS_TIME(constants.getMinGenesisTime().toString());
          }
        };

    GweiValuesData gweiValues =
        new GweiValuesData() {
          {
            setEJECTION_BALANCE(Long.toUnsignedString(constants.getEjectionBalance().getValue()));
            setEFFECTIVE_BALANCE_INCREMENT(
                Long.toUnsignedString(constants.getEffectiveBalanceIncrement().getValue()));
            setMIN_DEPOSIT_AMOUNT(
                Long.toUnsignedString(constants.getMinDepositAmount().getValue()));
            setMAX_EFFECTIVE_BALANCE(
                Long.toUnsignedString(constants.getMaxEffectiveBalance().getValue()));
          }
        };

    RewardAndPenaltyQuotientsData rewardAndPenaltyQuotients =
        new RewardAndPenaltyQuotientsData() {
          {
            setBASE_REWARD_FACTOR(constants.getBaseRewardFactor().toString());
            setINACTIVITY_PENALTY_QUOTIENT(constants.getInactivityPenaltyQuotient().toString());
            setWHISTLEBLOWER_REWARD_QUOTIENT(constants.getWhistleblowerRewardQuotient().toString());
            setPROPOSER_REWARD_QUOTIENT(
                constants.getProposerRewardQuotient().toString());
            setMIN_SLASHING_PENALTY_QUOTIENT(constants.getMinSlashingPenaltyQuotient().toString());
          }
        };

    StateListLengthsData stateListLengths =
        new StateListLengthsData() {
          {
            setEPOCHS_PER_HISTORICAL_VECTOR(constants.getEpochsPerHistoricalVector().toString());
            setEPOCHS_PER_SLASHINGS_VECTOR(constants.getEpochsPerSlashingsVector().toString());
            setHISTORICAL_ROOTS_LIMIT(constants.getHistoricalRootsLimit().toString());
            setVALIDATOR_REGISTRY_LIMIT(constants.getValidatorRegistryLimit().toString());
          }
        };

    TimeParametersData timeParameters =
        new TimeParametersData() {
          {
            setMIN_ATTESTATION_INCLUSION_DELAY(
                Long.toUnsignedString(constants.getMinAttestationInclusionDelay().getValue()));
            setMAX_SEED_LOOKAHEAD(constants.getMaxSeedLookahead().toString());
            setSLOTS_PER_ETH1_VOTING_PERIOD(constants.getSlotsPerEth1VotingPeriod().toString());
            setMIN_SEED_LOOKAHEAD(constants.getMinSeedLookahead().toString());
            setMIN_VALIDATOR_WITHDRAWABILITY_DELAY(
                constants.getMinValidatorWithdrawabilityDelay().toString());
            setSECONDS_PER_SLOT(Long.toString(constants.getSecondsPerSlot().getValue()));
            setSLOTS_PER_EPOCH(Long.toUnsignedString(constants.getSlotsPerEpoch().getValue()));
            setPERSISTENT_COMMITTEE_PERIOD(constants.getPersistentCommitteePeriod().toString());
            setMAX_EPOCHS_PER_CROSSLINK(constants.getMaxEpochsPerCrosslink().toString());
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
