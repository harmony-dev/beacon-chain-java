package org.ethereum.beacon.test.runner;

import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.test.type.StateTestCase;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;
import static org.ethereum.beacon.test.SilentAsserts.assertLists;
import static org.ethereum.beacon.test.SilentAsserts.assertTrue;

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
        "Current shuffling epoch doesn't match: ", this::compareCurrentShufflingEpoch, error);
    runComparison(
        "Current shuffling seed doesn't match: ", this::compareCurrentShufflingSeed, error);
    runComparison(
        "Current shuffling start shard doesn't match: ",
        this::compareCurrentShufflingStartShard,
        error);
    runComparison(
        "Previous justified epoch doesn't match: ", this::comparePreviousJustifiedEpoch, error);
    runComparison(
        "Previous justified root doesn't match: ", this::comparePreviousJustifiedRoot, error);
    runComparison(
        "Previous shuffling epoch doesn't match: ", this::comparePreviousShufflingEpoch, error);
    runComparison(
        "Previous shuffling seed doesn't match: ", this::comparePreviousShufflingSeed, error);
    runComparison(
        "Previous shuffling start shard doesn't match: ",
        this::comparePreviousShufflingStartShard,
        error);
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
    runComparison("Latest crosslinks do not match: ", this::compareLatestCrosslinks, error);
    runComparison("Latest Eth1 data doesn't match: ", this::compareLatestEth1Data, error);
    runComparison("Latest randao mixes do not match: ", this::compareLatestRandaoMixes, error);
    runComparison("Latest slashed balances do not match: ", this::compareSlashedBalances, error);
    runComparison("Latest state roots do not match: ", this::compareLatestStateRoots, error);
    runComparison(
        "Validator registry update epoch doesn't match: ",
        this::compareValidatorRegistryUpdateEpoch,
        error);

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
        expected.getLatestBlockRoots().stream()
            .map(Hash32::fromHexString)
            .collect(Collectors.toList()),
        actual.getLatestBlockRoots().listCopy());
  }

  private Optional<String> compareValidatorBalances() {
    if (expected.getValidatorBalances() == null) {
      return Optional.empty();
    }

    return assertLists(
        expected.getValidatorBalances(),
        actual.getValidatorBalances().stream()
            .map((el) -> Long.toString(el.getValue()))
            .collect(Collectors.toList()));
  }

  private Optional<String> compareSlashedBalances() {
    if (expected.getLatestSlashedBalances() == null) {
      return Optional.empty();
    }

    return assertLists(
        expected.getLatestSlashedBalances().stream()
            .map(UInt64::valueOf)
            .map(Gwei::new)
            .collect(Collectors.toList()),
        actual.getLatestSlashedBalances().listCopy());
  }

  private Optional<String> compareValidatorRegistry() {
    if (expected.getValidatorRegistry() == null) {
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
    if (expected.getCurrentEpochAttestations() == null) {
      return Optional.empty();
    }

    List<PendingAttestation> expectedAttestations =
        expected.getCurrentEpochAttestations().stream()
            .map(this::deserializeAttestation)
            .collect(Collectors.toList());
    return assertLists(expectedAttestations, actual.getCurrentEpochAttestations().listCopy());
  }

  private Optional<String> compareHistoricalRoots() {
    if (expected.getHistoricalRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots =
        expected.getHistoricalRoots().stream()
            .map(Hash32::fromHexString)
            .collect(Collectors.toList());
    return assertLists(expectedRoots, actual.getHistoricalRoots().listCopy());
  }

  private Optional<String> compareLatestRandaoMixes() {
    if (expected.getLatestRandaoMixes() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRandaoMixes =
        expected.getLatestRandaoMixes().stream()
            .map(Hash32::fromHexString)
            .collect(Collectors.toList());
    return assertLists(expectedRandaoMixes, actual.getLatestRandaoMixes().listCopy());
  }

  private Optional<String> compareLatestStateRoots() {
    if (expected.getLatestStateRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots =
        expected.getLatestStateRoots().stream()
            .map(Hash32::fromHexString)
            .collect(Collectors.toList());
    return assertLists(expectedRoots, actual.getLatestStateRoots().listCopy());
  }

  private Optional<String> compareLatestActiveIndexRoots() {
    if (expected.getLatestActiveIndexRoots() == null) {
      return Optional.empty();
    }

    List<Hash32> expectedRoots =
        expected.getLatestActiveIndexRoots().stream()
            .map(Hash32::fromHexString)
            .collect(Collectors.toList());
    return assertLists(expectedRoots, actual.getLatestActiveIndexRoots().listCopy());
  }

  private Optional<String> compareLatestCrosslinks() {
    if (expected.getLatestCrosslinks() == null) {
      return Optional.empty();
    }

    // FIXME: already modified by Michael, it couldn't match the test fixtures
    List<Crosslink> expectedCrosslinks =
        expected.getLatestCrosslinks().stream()
            .map(
                (el) -> {
                  return new Crosslink(
                      EpochNumber.castFrom(UInt64.valueOf(el.getEpoch())),
                      Hash32.fromHexString(el.getCrosslinkDataRoot()));
                })
            .collect(Collectors.toList());
    return assertLists(expectedCrosslinks, actual.getCurrentEpochCrosslinks().listCopy());
  }

  private PendingAttestation deserializeAttestation(
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

    return new PendingAttestation(
        Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
        attestationData1,
        Bitfield.of(BytesValue.fromHexString(attestationData.getCustodyBitfield())),
        SlotNumber.castFrom(UInt64.valueOf(attestationData.getInclusionSlot())));
  }

  private Optional<String> comparePreviousEpochAttestations() {
    if (expected.getPreviousEpochAttestations() == null) {
      return Optional.empty();
    }

    List<PendingAttestation> expectedAttestations =
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

  private Optional<String> compareCurrentShufflingEpoch() {
    if (expected.getCurrentShufflingEpoch() == null) {
      return Optional.empty();
    }

    return assertEquals(
        EpochNumber.castFrom(UInt64.valueOf(expected.getCurrentShufflingEpoch())),
        actual.getCurrentShufflingEpoch());
  }

  private Optional<String> compareCurrentShufflingSeed() {
    if (expected.getCurrentShufflingSeed() == null) {
      return Optional.empty();
    }

    return assertEquals(
        Hash32.fromHexString(expected.getCurrentShufflingSeed()), actual.getCurrentShufflingSeed());
  }

  private Optional<String> compareCurrentShufflingStartShard() {
    if (expected.getCurrentShufflingStartShard() == null) {
      return Optional.empty();
    }

    return assertEquals(
        expected.getCurrentShufflingStartShard(),
        actual.getCurrentShufflingStartShard().intValue());
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

  private Optional<String> comparePreviousShufflingEpoch() {
    if (expected.getPreviousShufflingEpoch() == null) {
      return Optional.empty();
    }

    return assertEquals(
        EpochNumber.castFrom(UInt64.valueOf(expected.getPreviousShufflingEpoch())),
        actual.getPreviousShufflingEpoch());
  }

  private Optional<String> compareValidatorRegistryUpdateEpoch() {
    if (expected.getValidatorRegistryUpdateEpoch() == null) {
      return Optional.empty();
    }

    return assertEquals(
        EpochNumber.castFrom(UInt64.valueOf(expected.getValidatorRegistryUpdateEpoch())),
        actual.getValidatorRegistryUpdateEpoch());
  }

  private Optional<String> comparePreviousShufflingSeed() {
    if (expected.getPreviousShufflingSeed() == null) {
      return Optional.empty();
    }

    return assertEquals(
        Hash32.fromHexString(expected.getPreviousShufflingSeed()),
        actual.getPreviousShufflingSeed());
  }

  private Optional<String> comparePreviousShufflingStartShard() {
    if (expected.getPreviousShufflingStartShard() == null) {
      return Optional.empty();
    }

    return assertEquals(
        expected.getPreviousShufflingStartShard(),
        actual.getPreviousShufflingStartShard().intValue());
  }

  private Optional<String> compareDepositIndex() {
    if (expected.getDepositIndex() == null) {
      return Optional.empty();
    }

    return assertEquals(expected.getDepositIndex(), actual.getDepositIndex().intValue());
  }

  private Optional<String> compareEth1DataVotes() {
    if (expected.getEth1DataVotes() == null) {
      return Optional.empty();
    }
    // XXX: not used in tests
    return assertTrue(
        "Expected that eth1DataVotes is empty but it's not true",
        actual.getEth1DataVotes().isEmpty());
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

    return assertEquals(
        new Fork(
            Bytes4.fromHexString(expected.getFork().getPreviousVersion()),
            Bytes4.fromHexString(expected.getFork().getCurrentVersion()),
            EpochNumber.castFrom(UInt64.valueOf(expected.getFork().getEpoch()))),
        actual.getFork());
  }

  private Optional<String> compareJustificationBitfield() {
    if (expected.getJustificationBitfield() == null) {
      return Optional.empty();
    }

    return assertEquals(
        Bitfield64.fromBytesLittleEndian(Bytes8.fromHexString(expected.getJustificationBitfield())),
        actual.getJustificationBitfield());
  }

  private Optional<String> compareLatestBlockHeader() {
    if (expected.getLatestBlockHeader() == null) {
      return Optional.empty();
    }

    BeaconBlockHeader expectedHeader =
        new BeaconBlockHeader(
            SlotNumber.castFrom(UInt64.valueOf(expected.getLatestBlockHeader().getSlot())),
            Hash32.fromHexString(expected.getLatestBlockHeader().getPreviousBlockRoot()),
            Hash32.fromHexString(expected.getLatestBlockHeader().getStateRoot()),
            Hash32.fromHexString(expected.getLatestBlockHeader().getBlockBodyRoot()),
            expected.getLatestBlockHeader().getSignature() == null
                ? BLSSignature.ZERO
                : BLSSignature.wrap(
                    Bytes96.fromHexString(expected.getLatestBlockHeader().getSignature())));

    return assertEquals(expectedHeader, actual.getLatestBlockHeader());
  }

  private Optional<String> compareLatestEth1Data() {
    if (expected.getLatestEth1Data() == null) {
      return Optional.empty();
    }

    Eth1Data expectedData =
        new Eth1Data(
            Hash32.fromHexString(expected.getLatestEth1Data().getDepositRoot()),
            Hash32.fromHexString(expected.getLatestEth1Data().getBlockHash()));
    return assertEquals(expectedData, actual.getLatestEth1Data());
  }
}
