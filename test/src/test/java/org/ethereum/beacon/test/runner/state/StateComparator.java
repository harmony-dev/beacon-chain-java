package org.ethereum.beacon.test.runner.state;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertLists;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.test.StateTestUtils;
import org.ethereum.beacon.test.type.state.StateTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class StateComparator {
  private BeaconState actual;
  private StateTestCase.BeaconStateData expected;

  public static Optional<String> compare(StateTestCase.BeaconStateData expected, BeaconState actual) {
    return new StateComparator(expected, actual).compare();
  }

  private StateComparator(StateTestCase.BeaconStateData expected, BeaconState actual) {
    this.expected = expected;
    this.actual = actual;
  }

  public Optional<String> compare() {
    StringBuilder error = new StringBuilder();

    // Validating result
    runComparison("Slot number doesn't match: ", this::compareSlotNumber, error);
    runComparison("Latest block roots do not match: ", this::compareLatestBlockRoots, error);
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
        "Current justified epoch doesn't match: ", this::compareCurrentJustifiedEpoch, error);
    runComparison(
        "Current justified root doesn't match: ", this::compareCurrentJustifiedRoot, error);
    runComparison(
        "Previous justified epoch doesn't match: ", this::comparePreviousJustifiedEpoch, error);
    runComparison(
        "Previous justified root doesn't match: ", this::comparePreviousJustifiedRoot, error);
    runComparison("Deposit index doesn't match: ", this::compareDepositIndex, error);
    runComparison("Eth1 data votes doesn't match: ", this::compareEth1DataVotes, error);
    runComparison("Finalized epoch doesn't match: ", this::compareFinalizedEpoch, error);
    runComparison("Finalized root doesn't match: ", this::compareFinalizedRoot, error);
    runComparison("Fork doesn't match: ", this::compareFork, error);
    runComparison("Historical roots do not match: ", this::compareHistoricalRoots, error);
    runComparison(
        "Justification bitfield doesn't match: ", this::compareJustificationBitfield, error);
    runComparison(
        "Latest active index roots do not match: ", this::compareLatestActiveIndexRoots, error);
    runComparison("Latest block header doesn't match: ", this::compareLatestBlockHeader, error);
    runComparison("Current crosslinks do not match: ", this::compareCurrentCrosslinks, error);
    runComparison("Previous crosslinks do not match: ", this::comparePreviousCrosslinks, error);
    runComparison("Latest Eth1 data doesn't match: ", this::compareLatestEth1Data, error);
    runComparison("Latest randao mixes do not match: ", this::compareLatestRandaoMixes, error);
    runComparison("Latest slashed balances do not match: ", this::compareSlashedBalances, error);
    runComparison("Latest state roots do not match: ", this::compareLatestStateRoots, error);

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

  private Optional<String> compareLatestBlockRoots() {
    if (expected.getLatestBlockRoots() == null) {
      return Optional.empty();
    }

    return assertLists(
        StateTestUtils.parseHashes(expected.getLatestBlockRoots()),
        actual.getLatestBlockRoots().listCopy());
  }

  private Optional<String> compareValidatorBalances() {
    if (expected.getBalances() == null) {
      return Optional.empty();
    }

    return assertLists(
        StateTestUtils.parseBalances(expected.getBalances()),
        actual.getBalances().listCopy());
  }

  private Optional<String> compareSlashedBalances() {
    if (expected.getLatestSlashedBalances() == null) {
      return Optional.empty();
    }

    return assertLists(
        StateTestUtils.parseBalances(expected.getLatestSlashedBalances()),
        actual.getLatestSlashedBalances().listCopy());
  }

  private Optional<String> compareValidatorRegistry() {
    if (expected.getValidatorRegistry() == null) {
      return Optional.empty();
    }

    List<ValidatorRecord> expectedValidators =
        StateTestUtils.parseValidatorRegistry(expected.getValidatorRegistry());
    return assertLists(expectedValidators, actual.getValidatorRegistry().listCopy());
  }

  private Optional<String> compareCurrentEpochAttestations() {
    if (expected.getCurrentEpochAttestations() == null) {
      return Optional.empty();
    }

    List<PendingAttestation> expectedAttestations =
        StateTestUtils.parsePendingAttestations(expected.getCurrentEpochAttestations());
    return assertLists(expectedAttestations, actual.getCurrentEpochAttestations().listCopy());
  }

  private Optional<String> compareHistoricalRoots() {
    if (expected.getHistoricalRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots = StateTestUtils.parseHashes(expected.getHistoricalRoots());
    return assertLists(expectedRoots, actual.getHistoricalRoots().listCopy());
  }

  private Optional<String> compareLatestRandaoMixes() {
    if (expected.getLatestRandaoMixes() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRandaoMixes = StateTestUtils.parseHashes(expected.getLatestRandaoMixes());
    return assertLists(expectedRandaoMixes, actual.getLatestRandaoMixes().listCopy());
  }

  private Optional<String> compareLatestStateRoots() {
    if (expected.getLatestStateRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots = StateTestUtils.parseHashes(expected.getLatestStateRoots());
    return assertLists(expectedRoots, actual.getLatestStateRoots().listCopy());
  }

  private Optional<String> compareLatestActiveIndexRoots() {
    if (expected.getLatestActiveIndexRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots = StateTestUtils.parseHashes(expected.getLatestActiveIndexRoots());
    return assertLists(expectedRoots, actual.getLatestActiveIndexRoots().listCopy());
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
        StateTestUtils.parsePendingAttestations(expected.getPreviousEpochAttestations());
    return assertLists(expectedAttestations, actual.getPreviousEpochAttestations().listCopy());
  }

  private Optional<String> compareCurrentJustifiedEpoch() {
    if (expected.getCurrentJustifiedEpoch() == null) {
      return Optional.empty();
    }

    return assertEquals(
        EpochNumber.castFrom(UInt64.valueOf(expected.getCurrentJustifiedEpoch())),
        actual.getCurrentJustifiedEpoch());
  }

  private Optional<String> compareCurrentJustifiedRoot() {
    if (expected.getCurrentJustifiedRoot() == null) {
      return Optional.empty();
    }

    return assertEquals(
        Hash32.fromHexString(expected.getCurrentJustifiedRoot()), actual.getCurrentJustifiedRoot());
  }

  private Optional<String> compareGenesisTime() {
    if (expected.getGenesisTime() == null) {
      return Optional.empty();
    }

    return assertEquals(expected.getGenesisTime(), actual.getGenesisTime().getValue());
  }

  private Optional<String> comparePreviousJustifiedEpoch() {
    if (expected.getPreviousJustifiedEpoch() == null) {
      return Optional.empty();
    }

    return assertEquals(
        EpochNumber.castFrom(UInt64.valueOf(expected.getPreviousJustifiedEpoch())),
        actual.getPreviousJustifiedEpoch());
  }

  private Optional<String> comparePreviousJustifiedRoot() {
    if (expected.getPreviousJustifiedRoot() == null) {
      return Optional.empty();
    }

    return assertEquals(
        Hash32.fromHexString(expected.getPreviousJustifiedRoot()),
        actual.getPreviousJustifiedRoot());
  }

  private Optional<String> compareDepositIndex() {
    if (expected.getDepositIndex() == null) {
      return Optional.empty();
    }

    return assertEquals(expected.getDepositIndex(), actual.getDepositIndex().getValue());
  }

  private Optional<String> compareEth1DataVotes() {
    if (expected.getEth1DataVotes() == null) {
      return Optional.empty();
    }
    List<Eth1Data> expectedVotes = expected.getEth1DataVotes().stream().map(StateTestUtils::parseEth1Data).collect(Collectors.toList());

    return assertLists(expectedVotes, actual.getEth1DataVotes().listCopy());
  }

  private Optional<String> compareFinalizedEpoch() {
    if (expected.getFinalizedEpoch() == null) {
      return Optional.empty();
    }

    return assertEquals(
        EpochNumber.castFrom(UInt64.valueOf(expected.getFinalizedEpoch())),
        actual.getFinalizedEpoch());
  }

  private Optional<String> compareFinalizedRoot() {
    if (expected.getFinalizedRoot() == null) {
      return Optional.empty();
    }

    return assertEquals(
        Hash32.fromHexString(expected.getFinalizedRoot()), actual.getFinalizedRoot());
  }

  private Optional<String> compareFork() {
    if (expected.getFork() == null) {
      return Optional.empty();
    }

    return assertEquals(StateTestUtils.parseFork(expected.getFork()), actual.getFork());
  }

  private Optional<String> compareJustificationBitfield() {
    if (expected.getJustificationBitfield() == null) {
      return Optional.empty();
    }

    return assertEquals(
        new Bitfield64(UInt64.valueOf(expected.getJustificationBitfield())),
        actual.getJustificationBitfield());
  }

  private Optional<String> compareLatestBlockHeader() {
    if (expected.getLatestBlockHeader() == null) {
      return Optional.empty();
    }

    BeaconBlockHeader expectedHeader =
        StateTestUtils.parseBeaconBlockHeader(expected.getLatestBlockHeader());

    return assertEquals(expectedHeader, actual.getLatestBlockHeader());
  }

  private Optional<String> compareLatestEth1Data() {
    if (expected.getLatestEth1Data() == null) {
      return Optional.empty();
    }

    Eth1Data expectedData = StateTestUtils.parseEth1Data(expected.getLatestEth1Data());
    return assertEquals(expectedData, actual.getLatestEth1Data());
  }
}
