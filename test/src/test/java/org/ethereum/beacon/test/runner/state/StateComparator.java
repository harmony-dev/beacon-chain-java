package org.ethereum.beacon.test.runner.state;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitvector;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.state.StateTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertLists;

public class StateComparator {
  private BeaconState actual;
  private StateTestCase.BeaconStateData expected;
  private BeaconChainSpec spec;

  private StateComparator(
      StateTestCase.BeaconStateData expected, BeaconState actual, BeaconChainSpec spec) {
    this.expected = expected;
    this.actual = actual;
    this.spec = spec;
  }

  public static Optional<String> compare(
      StateTestCase.BeaconStateData expected, BeaconState actual, BeaconChainSpec spec) {
    return new StateComparator(expected, actual, spec).compare();
  }

  public Optional<String> compare() {
    StringBuilder error = new StringBuilder();

    // Validating result
    runComparison("Slot number doesn't match: ", this::compareSlotNumber, error);
    runComparison("Block roots do not match: ", this::compareBlockRoots, error);
    runComparison("Validator balances do not match: ", this::compareValidatorBalances, error);
    runComparison("Validator registries do not match: ", this::compareValidatorRegistry, error);
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
    if (expected.getSlot() == null) {
      return Optional.empty();
    }

    return assertEquals(SlotNumber.castFrom(UInt64.valueOf(expected.getSlot())), actual.getSlot());
  }

  private Optional<String> compareBlockRoots() {
    if (expected.getBlockRoots() == null) {
      return Optional.empty();
    }

    return assertLists(
        StateTestUtils.parseHashes(expected.getBlockRoots()), actual.getBlockRoots().listCopy());
  }

  private Optional<String> compareValidatorBalances() {
    if (expected.getBalances() == null) {
      return Optional.empty();
    }

    return assertLists(
        StateTestUtils.parseBalances(expected.getBalances()), actual.getBalances().listCopy());
  }

  private Optional<String> compareSlashedBalances() {
    if (expected.getSlashings() == null) {
      return Optional.empty();
    }

    return assertLists(
        StateTestUtils.parseBalances(expected.getSlashings()), actual.getSlashings().listCopy());
  }

  private Optional<String> compareValidatorRegistry() {
    if (expected.getValidators() == null) {
      return Optional.empty();
    }

    List<ValidatorRecord> expectedValidators =
        StateTestUtils.parseValidatorRegistry(expected.getValidators());
    return assertLists(expectedValidators, actual.getValidators().listCopy());
  }

  private Optional<String> compareCurrentEpochAttestations() {
    if (expected.getCurrentEpochAttestations() == null) {
      return Optional.empty();
    }

    List<PendingAttestation> expectedAttestations =
        StateTestUtils.parsePendingAttestations(expected.getCurrentEpochAttestations(), spec.getConstants());
    return assertLists(expectedAttestations, actual.getCurrentEpochAttestations().listCopy());
  }

  private Optional<String> compareHistoricalRoots() {
    if (expected.getHistoricalRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots = StateTestUtils.parseHashes(expected.getHistoricalRoots());
    return assertLists(expectedRoots, actual.getHistoricalRoots().listCopy());
  }

  private Optional<String> compareRandaoMixes() {
    if (expected.getRandaoMixes() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRandaoMixes = StateTestUtils.parseHashes(expected.getRandaoMixes());
    return assertLists(expectedRandaoMixes, actual.getRandaoMixes().listCopy());
  }

  private Optional<String> compareStateRoots() {
    if (expected.getStateRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots = StateTestUtils.parseHashes(expected.getStateRoots());
    return assertLists(expectedRoots, actual.getStateRoots().listCopy());
  }

  private Optional<String> compareActiveIndexRoots() {
    if (expected.getActiveIndexRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots = StateTestUtils.parseHashes(expected.getActiveIndexRoots());
    return assertLists(expectedRoots, actual.getActiveIndexRoots().listCopy());
  }

  private Optional<String> compareCompactCommitteesRoots() {
    if (expected.getCompactCommitteesRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots = StateTestUtils.parseHashes(expected.getCompactCommitteesRoots());
    return assertLists(expectedRoots, actual.getCompactCommitteesRoots().listCopy());
  }

  private Optional<String> compareCurrentCrosslinks() {
    if (expected.getCurrentCrosslinks() == null) {
      return Optional.empty();
    }

    List<Crosslink> expectedCrosslinks =
        StateTestUtils.parseCrosslinks(expected.getCurrentCrosslinks());
    return assertLists(expectedCrosslinks, actual.getCurrentCrosslinks().listCopy());
  }

  private Optional<String> comparePreviousCrosslinks() {
    if (expected.getPreviousCrosslinks() == null) {
      return Optional.empty();
    }

    List<Crosslink> expectedCrosslinks =
        StateTestUtils.parseCrosslinks(expected.getPreviousCrosslinks());
    return assertLists(expectedCrosslinks, actual.getPreviousCrosslinks().listCopy());
  }

  private Optional<String> comparePreviousEpochAttestations() {
    if (expected.getPreviousEpochAttestations() == null) {
      return Optional.empty();
    }

    List<PendingAttestation> expectedAttestations =
        StateTestUtils.parsePendingAttestations(expected.getPreviousEpochAttestations(), spec.getConstants());
    return assertLists(expectedAttestations, actual.getPreviousEpochAttestations().listCopy());
  }

  private Optional<String> compareCurrentJustifiedCheckpoint() {
    if (expected.getCurrentJustifiedCheckpoint() == null) {
      return Optional.empty();
    }
    Checkpoint expectedCheckpoint =
        StateTestUtils.parseCheckpoint(expected.getCurrentJustifiedCheckpoint());

    return assertEquals(expectedCheckpoint, actual.getCurrentJustifiedCheckpoint());
  }

  private Optional<String> compareGenesisTime() {
    if (expected.getGenesisTime() == null) {
      return Optional.empty();
    }

    return assertEquals(expected.getGenesisTime(), actual.getGenesisTime().getValue());
  }

  private Optional<String> comparePreviousJustifiedCheckpoint() {
    if (expected.getPreviousJustifiedCheckpoint() == null) {
      return Optional.empty();
    }
    Checkpoint expectedCheckpoint =
        StateTestUtils.parseCheckpoint(expected.getPreviousJustifiedCheckpoint());

    return assertEquals(expectedCheckpoint, actual.getPreviousJustifiedCheckpoint());
  }

  private Optional<String> compareDepositIndex() {
    if (expected.getEth1DepositIndex() == null) {
      return Optional.empty();
    }

    return assertEquals(expected.getEth1DepositIndex(), actual.getEth1DepositIndex().getValue());
  }

  private Optional<String> compareEth1DataVotes() {
    if (expected.getEth1DataVotes() == null) {
      return Optional.empty();
    }
    List<Eth1Data> expectedVotes =
        expected.getEth1DataVotes().stream()
            .map(StateTestUtils::parseEth1Data)
            .collect(Collectors.toList());

    return assertLists(expectedVotes, actual.getEth1DataVotes().listCopy());
  }

  private Optional<String> compareFinalizedCheckpoint() {
    if (expected.getFinalizedCheckpoint() == null) {
      return Optional.empty();
    }
    Checkpoint expectedCheckpoint =
        StateTestUtils.parseCheckpoint(expected.getFinalizedCheckpoint());

    return assertEquals(expectedCheckpoint, actual.getFinalizedCheckpoint());
  }

  private Optional<String> compareFork() {
    if (expected.getFork() == null) {
      return Optional.empty();
    }

    return assertEquals(StateTestUtils.parseFork(expected.getFork()), actual.getFork());
  }

  private Optional<String> compareJustificationBitfield() {
    if (expected.getJustificationBits() == null) {
      return Optional.empty();
    }

    return assertEquals(
        Bitvector.of(
            spec.getConstants().getJustificationBitsLength(),
            BytesValue.fromHexString(expected.getJustificationBits())),
        actual.getJustificationBits());
  }

  private Optional<String> compareLatestBlockHeader() {
    if (expected.getLatestBlockHeader() == null) {
      return Optional.empty();
    }

    BeaconBlockHeader expectedHeader =
        StateTestUtils.parseBeaconBlockHeader(expected.getLatestBlockHeader());

    return assertEquals(expectedHeader, actual.getLatestBlockHeader());
  }

  private Optional<String> compareEth1Data() {
    if (expected.getEth1Data() == null) {
      return Optional.empty();
    }

    Eth1Data expectedData = StateTestUtils.parseEth1Data(expected.getEth1Data());
    return assertEquals(expectedData, actual.getEth1Data());
  }
}
