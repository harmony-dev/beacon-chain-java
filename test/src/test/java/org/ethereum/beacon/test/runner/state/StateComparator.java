package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconState;

import java.util.Optional;
import java.util.function.Supplier;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertLists;

public class StateComparator {
  private BeaconState expected;
  private BeaconState actual;
  private BeaconChainSpec spec;

  private StateComparator(BeaconState expected, BeaconState actual, BeaconChainSpec spec) {
    this.expected = expected;
    this.actual = actual;
    this.spec = spec;
  }

  public static Optional<String> compare(
      BeaconState expected, BeaconState actual, BeaconChainSpec spec) {
    return new StateComparator(expected, actual, spec).compare();
  }

  public Optional<String> compare() {
    StringBuilder error = new StringBuilder();

    // Validating result
    runComparison("Slot number doesn't match: ", this::compareSlotNumber, error);
    runComparison("Block roots do not match: ", this::compareBlockRoots, error);
    runComparison("Validators do not match: ", this::compareValidators, error);
    runComparison("Validator balances do not match: ", this::compareValidatorBalances, error);
    runComparison("Start shard doesn't match: ", this::compareStartShard, error);
    runComparison("Genesis time doesn't match: ", this::compareGenesisTime, error);
    runComparison(
        "Current epoch attestations do not match: ", this::compareCurrentEpochAttestations, error);
    runComparison(
        "Previous epoch attestations do not match: ",
        this::comparePreviousEpochAttestations,
        error);
    runComparison(
        "Current justified checkpoint doesn't match: ",
        this::compareCurrentJustifiedCheckpoint,
        error);
    runComparison(
        "Previous justified checkpoint doesn't match: ",
        this::comparePreviousJustifiedCheckpoint,
        error);
    runComparison("Deposit index doesn't match: ", this::compareDepositIndex, error);
    runComparison("Eth1 data votes doesn't match: ", this::compareEth1DataVotes, error);
    runComparison("Finalized checkpoint doesn't match: ", this::compareFinalizedCheckpoint, error);
    runComparison("Fork doesn't match: ", this::compareFork, error);
    runComparison("Historical roots do not match: ", this::compareHistoricalRoots, error);
    runComparison(
        "Justification bitfield doesn't match: ", this::compareJustificationBitfield, error);
    runComparison("Active index roots do not match: ", this::compareActiveIndexRoots, error);
    runComparison(
        "Compact committees roots do not match: ", this::compareCompactCommitteesRoots, error);
    runComparison("Latest block header doesn't match: ", this::compareLatestBlockHeader, error);
    runComparison("Current crosslinks do not match: ", this::compareCurrentCrosslinks, error);
    runComparison("Previous crosslinks do not match: ", this::comparePreviousCrosslinks, error);
    runComparison("Eth1 data doesn't match: ", this::compareEth1Data, error);
    runComparison("Randao mixes do not match: ", this::compareRandaoMixes, error);
    runComparison("Slashed balances do not match: ", this::compareSlashedBalances, error);
    runComparison("State roots do not match: ", this::compareStateRoots, error);

    return error.length() == 0 ? Optional.empty() : Optional.of(error.toString());
  }

  private void runComparison(String msg, Supplier<Optional<String>> method, StringBuilder errors) {
    try {
      method
          .get()
          .ifPresent(
              (error) -> {
                errors.append(msg).append(error).append("\n");
              });
    } catch (Exception ex) {
      errors.append(msg).append(ex.toString()).append("\n");
    }
  }

  private Optional<String> compareSlotNumber() {
    return assertEquals(expected.getSlot(), actual.getSlot());
  }

  private Optional<String> compareBlockRoots() {
    return assertLists(expected.getBlockRoots().listCopy(), actual.getBlockRoots().listCopy());
  }

  private Optional<String> compareValidatorBalances() {
    return assertLists(expected.getBalances().listCopy(), actual.getBalances().listCopy());
  }

  private Optional<String> compareSlashedBalances() {
    return assertLists(expected.getSlashings().listCopy(), actual.getSlashings().listCopy());
  }

  private Optional<String> compareValidators() {
    return assertLists(expected.getValidators().listCopy(), actual.getValidators().listCopy());
  }

  private Optional<String> compareCurrentEpochAttestations() {
    return assertLists(
        expected.getCurrentEpochAttestations().listCopy(),
        actual.getCurrentEpochAttestations().listCopy());
  }

  private Optional<String> compareHistoricalRoots() {
    return assertLists(
        expected.getHistoricalRoots().listCopy(), actual.getHistoricalRoots().listCopy());
  }

  private Optional<String> compareRandaoMixes() {
    return assertLists(expected.getRandaoMixes().listCopy(), actual.getRandaoMixes().listCopy());
  }

  private Optional<String> compareStateRoots() {
    return assertLists(expected.getStateRoots().listCopy(), actual.getStateRoots().listCopy());
  }

  private Optional<String> compareActiveIndexRoots() {
    return assertLists(
        expected.getActiveIndexRoots().listCopy(), actual.getActiveIndexRoots().listCopy());
  }

  private Optional<String> compareCompactCommitteesRoots() {
    return assertLists(
        expected.getCompactCommitteesRoots().listCopy(),
        actual.getCompactCommitteesRoots().listCopy());
  }

  private Optional<String> compareCurrentCrosslinks() {
    return assertLists(
        expected.getCurrentCrosslinks().listCopy(), actual.getCurrentCrosslinks().listCopy());
  }

  private Optional<String> comparePreviousCrosslinks() {
    return assertLists(
        expected.getPreviousCrosslinks().listCopy(), actual.getPreviousCrosslinks().listCopy());
  }

  private Optional<String> comparePreviousEpochAttestations() {
    return assertLists(
        expected.getPreviousEpochAttestations().listCopy(),
        actual.getPreviousEpochAttestations().listCopy());
  }

  private Optional<String> compareCurrentJustifiedCheckpoint() {
    return assertEquals(
        expected.getCurrentJustifiedCheckpoint(), actual.getCurrentJustifiedCheckpoint());
  }

  private Optional<String> compareGenesisTime() {
    return assertEquals(expected.getGenesisTime(), actual.getGenesisTime());
  }

  private Optional<String> compareStartShard() {
    return assertEquals(expected.getStartShard(), actual.getStartShard());
  }

  private Optional<String> comparePreviousJustifiedCheckpoint() {
    return assertEquals(
        expected.getPreviousJustifiedCheckpoint(), actual.getPreviousJustifiedCheckpoint());
  }

  private Optional<String> compareDepositIndex() {
    return assertEquals(expected.getEth1DepositIndex(), actual.getEth1DepositIndex());
  }

  private Optional<String> compareEth1DataVotes() {
    return assertLists(
        expected.getEth1DataVotes().listCopy(), actual.getEth1DataVotes().listCopy());
  }

  private Optional<String> compareFinalizedCheckpoint() {
    return assertEquals(expected.getFinalizedCheckpoint(), actual.getFinalizedCheckpoint());
  }

  private Optional<String> compareFork() {
    return assertEquals(expected.getFork(), actual.getFork());
  }

  private Optional<String> compareJustificationBitfield() {
    return assertEquals(expected.getJustificationBits(), actual.getJustificationBits());
  }

  private Optional<String> compareLatestBlockHeader() {
    return assertEquals(expected.getLatestBlockHeader(), actual.getLatestBlockHeader());
  }

  private Optional<String> compareEth1Data() {
    return assertEquals(expected.getEth1Data(), actual.getEth1Data());
  }
}
