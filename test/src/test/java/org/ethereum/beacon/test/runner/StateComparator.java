package org.ethereum.beacon.test.runner;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.test.type.StateTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertLists;

public class StateComparator {
  private BeaconStateEx actual;
  private StateTestCase.BeaconStateData expected;

  public StateComparator(StateTestCase.BeaconStateData expected, BeaconStateEx actual) {
    this.expected = expected;
    this.actual = actual;
  }

  public Optional<String> compare() {
    StringBuilder error = new StringBuilder();

    // Validating result
    runner("Slot number doesn't match: ", this::compareSlotNumber, error);
    runner("Latest block roots doesn't match: ", this::compareLatestBlockRoots, error);
    runner("Validator balances doesn't match: ", this::compareValidatorBalances, error);
    runner("Validator registries doesn't match: ", this::compareValidatorRegistry, error);
    runner("Genesis time doesn't match: ", this::compareGenesisTime, error);
    runner(
        "Current epoch attestations doesn't match: ", this::compareCurrentEpochAttestations, error);
    runner(
        "Previous epoch attestations doesn't match: ",
        this::comparePreviousEpochAttestations,
        error);
    runner("Current justified epoch doesn't match: ", this::compareCurrentJustifiedEpoch, error);
    runner("Current justified root doesn't match: ", this::compareCurrentJustifiedRoot, error);
    // TODO
    expected.getCurrentShufflingEpoch();
    expected.getCurrentShufflingSeed();
    expected.getCurrentShufflingStartShard();
    expected.getPreviousEpochAttestations();
    expected.getPreviousJustifiedEpoch();
    expected.getPreviousJustifiedRoot();
    expected.getPreviousShufflingEpoch();
    expected.getPreviousShufflingSeed();
    expected.getPreviousShufflingStartShard();
    expected.getDepositIndex();
    expected.getEth1DataVotes();
    expected.getFinalizedEpoch();
    expected.getFinalizedRoot();
    expected.getFork();
    expected.getHistoricalRoots();
    expected.getJustificationBitfield();
    expected.getLatestActiveIndexRoots();
    expected.getLatestBlockHeader();
    expected.getLatestCrosslinks();
    expected.getLatestEth1Data();
    expected.getLatestRandaoMixes();
    expected.getLatestSlashedBalances();
    expected.getLatestStateRoots();
    expected.getValidatorRegistryUpdateEpoch();

    return error.length() == 0 ? Optional.empty() : Optional.of(error.toString());
  }

  private void runner(String msg, Supplier<Optional<String>> method, StringBuilder errors) {
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
    if (expected.getLatestBlockRoots() == null || expected.getLatestBlockRoots().isEmpty()) {
      return Optional.empty();
    }

    List<String> actualBlockRoots = new ArrayList<>();
    actual.getLatestBlockRoots().stream()
        .forEach((root) -> actualBlockRoots.add("0x" + root.toString()));
    return assertLists(expected.getLatestBlockRoots(), actualBlockRoots);
  }

  private Optional<String> compareValidatorBalances() {
    if (expected.getValidatorBalances() == null || expected.getValidatorBalances().isEmpty()) {
      return Optional.empty();
    }

    return assertLists(
        expected.getValidatorBalances(),
        actual.getValidatorBalances().stream()
            .map((el) -> Long.toString(el.getValue()))
            .collect(Collectors.toList()));
  }

  private Optional<String> compareValidatorRegistry() {
    if (expected.getValidatorRegistry() == null || expected.getValidatorRegistry().isEmpty()) {
      return Optional.empty();
    }

    List<ValidatorRecord> expectedValidators =
        expected.getValidatorRegistry().stream()
            .map(
                (el) -> {
                  return new ValidatorRecord(
                      BLSPubkey.fromHexString(el.getPubkey()),
                      Hash32.fromHexString(el.getWithdrawalCredentials()),
                      EpochNumber.castFrom(UInt64.valueOf(el.getActivationEpoch())),
                      EpochNumber.castFrom(UInt64.valueOf(el.getExitEpoch())),
                      EpochNumber.castFrom(UInt64.valueOf(el.getWithdrawableEpoch())),
                      el.getInitiatedExit(),
                      el.getSlashed());
                })
            .collect(Collectors.toList());
    return assertLists(expectedValidators, actual.getValidatorRegistry().listCopy());
  }

  private Optional<String> compareCurrentEpochAttestations() {
    if (expected.getCurrentEpochAttestations() == null
        || expected.getCurrentEpochAttestations().isEmpty()) {
      return Optional.empty();
    }

    List<Attestation> expectedAttestations =
        expected.getCurrentEpochAttestations().stream()
            .map(this::deserializeAttestation)
            .collect(Collectors.toList());
    return assertLists(expectedAttestations, actual.getCurrentEpochAttestations().listCopy());
  }

  private Attestation deserializeAttestation(
      StateTestCase.BeaconStateData.AttestationData attestationData) {
    AttestationData attestationData1 =
        new AttestationData(
            SlotNumber.castFrom(UInt64.valueOf(attestationData.getData().getSlot())),
            Hash32.fromHexString(attestationData.getData().getBeaconBlockRoot()),
            EpochNumber.castFrom(UInt64.valueOf(attestationData.getData().getSourceEpoch())),
            Hash32.fromHexString(attestationData.getData().getSourceRoot()),
            Hash32.fromHexString(attestationData.getData().getTargetRoot()),
            ShardNumber.of(attestationData.getData().getShard()),
            new Crosslink(
                EpochNumber.castFrom(
                    UInt64.valueOf(attestationData.getData().getPreviousCrosslink().getEpoch())),
                Hash32.fromHexString(
                    attestationData.getData().getPreviousCrosslink().getCrosslinkDataRoot())),
            Hash32.fromHexString(attestationData.getData().getCrosslinkDataRoot()));

    return new Attestation(
        Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
        attestationData1,
        Bitfield.of(BytesValue.fromHexString(attestationData.getCustodyBitfield())),
        attestationData.getAggregateSignature() == null
            ? BLSSignature.ZERO
            : BLSSignature.wrap(Bytes96.fromHexString(attestationData.getAggregateSignature())));
  }

  private Optional<String> comparePreviousEpochAttestations() {
    if (expected.getPreviousEpochAttestations() == null
        || expected.getPreviousEpochAttestations().isEmpty()) {
      return Optional.empty();
    }

    List<Attestation> expectedAttestations =
        expected.getPreviousEpochAttestations().stream()
            .map(this::deserializeAttestation)
            .collect(Collectors.toList());
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
}
