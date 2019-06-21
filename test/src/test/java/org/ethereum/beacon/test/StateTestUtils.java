package org.ethereum.beacon.test;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.Bitfield64;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData.CrossLinkData;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData.ValidatorData;
import org.ethereum.beacon.validator.api.convert.BlockDataToBlock;
import org.ethereum.beacon.validator.api.model.BlockData;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.stream.Collectors;

/** Various utility methods aiding state tests development. */
public abstract class StateTestUtils {
  private StateTestUtils() {}

  public static BeaconBlock parseBlockData(BlockData blockData) {
    return BlockDataToBlock.deserialize(blockData);
  }

  public static MutableBeaconState parseBeaconState(
      SpecConstants specConstants, BeaconStateData data) {
    MutableBeaconState state = BeaconState.getEmpty(specConstants).createMutableCopy();

    state.setSlot(SlotNumber.castFrom(UInt64.valueOf(data.getSlot())));
    state.setGenesisTime(Time.of(data.getGenesisTime()));
    state.setFork(parseFork(data.getFork()));
    state.setPreviousJustifiedEpoch(
        EpochNumber.castFrom(UInt64.valueOf(data.getPreviousJustifiedEpoch())));
    state.setCurrentJustifiedEpoch(
        EpochNumber.castFrom(UInt64.valueOf(data.getCurrentJustifiedEpoch())));
    state.setPreviousJustifiedRoot(Hash32.fromHexString(data.getPreviousJustifiedRoot()));
    state.setCurrentJustifiedRoot(Hash32.fromHexString(data.getCurrentJustifiedRoot()));
    state.setJustificationBitfield(new Bitfield64(UInt64.valueOf(data.getJustificationBitfield())));
    state.setFinalizedEpoch(EpochNumber.castFrom(UInt64.valueOf(data.getFinalizedEpoch())));
    state.setFinalizedRoot(Hash32.fromHexString(data.getFinalizedRoot()));
    state.setLatestBlockHeader(parseBeaconBlockHeader(data.getLatestBlockHeader()));
    state.setLatestEth1Data(BlockDataToBlock.parseEth1Data(data.getLatestEth1Data()));
    state.setEth1DataVotes(
        WriteList.wrap(
            data.getEth1DataVotes().stream()
                .map(BlockDataToBlock::parseEth1Data)
                .collect(Collectors.toList()),
            Integer::new));
    state.setDepositIndex(UInt64.valueOf(data.getDepositIndex()));

    state.getValidatorRegistry().replaceAll(parseValidatorRegistry(data.getValidatorRegistry()));
    state.getBalances().replaceAll(parseBalances(data.getBalances()));
    state.getLatestRandaoMixes().setAll(parseHashes(data.getLatestRandaoMixes()));
    state
        .getPreviousEpochAttestations()
        .replaceAll(parsePendingAttestations(data.getPreviousEpochAttestations()));
    state
        .getCurrentEpochAttestations()
        .replaceAll(parsePendingAttestations(data.getCurrentEpochAttestations()));
    state.getCurrentCrosslinks().replaceAll(parseCrosslinks(data.getCurrentCrosslinks()));
    state.getPreviousCrosslinks().replaceAll(parseCrosslinks(data.getPreviousCrosslinks()));
    state.getLatestBlockRoots().setAll(parseHashes(data.getLatestBlockRoots()));
    state.getLatestStateRoots().setAll(parseHashes(data.getLatestStateRoots()));
    state.getLatestActiveIndexRoots().setAll(parseHashes(data.getLatestActiveIndexRoots()));
    state.getHistoricalRoots().replaceAll(parseHashes(data.getHistoricalRoots()));
    state.getLatestSlashedBalances().setAll(parseBalances(data.getLatestSlashedBalances()));
    state.setLatestStartShard(ShardNumber.of(UInt64.valueOf(data.getLatestStartShard())));

    return state;
  }

  public static List<Crosslink> parseCrosslinks(List<CrossLinkData> data) {
    return data.stream().map(StateTestUtils::parseCrosslink).collect(Collectors.toList());
  }

  public static List<PendingAttestation> parsePendingAttestations(
      List<BlockData.AttestationData> data) {
    return data.stream().map(StateTestUtils::parsePendingAttestation).collect(Collectors.toList());
  }

  public static List<Hash32> parseHashes(List<String> data) {
    return data.stream().map(Hash32::fromHexString).collect(Collectors.toList());
  }

  public static List<Gwei> parseBalances(List<String> data) {
    return data.stream().map(b -> Gwei.castFrom(UInt64.valueOf(b))).collect(Collectors.toList());
  }

  public static List<ValidatorRecord> parseValidatorRegistry(List<ValidatorData> data) {
    return data.stream().map(StateTestUtils::parseValidatorRecord).collect(Collectors.toList());
  }

  public static ValidatorRecord parseValidatorRecord(ValidatorData data) {
    return new ValidatorRecord(
        BLSPubkey.fromHexString(data.getPubkey()),
        Hash32.fromHexString(data.getWithdrawalCredentials()),
        EpochNumber.castFrom(UInt64.valueOf(data.getActivationEligibilityEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getActivationEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getExitEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getWithdrawableEpoch())),
        data.getSlashed(),
        Gwei.castFrom(UInt64.valueOf(data.getEffectiveBalance())));
  }

  public static Eth1Data parseEth1Data(BlockData.BlockBodyData.Eth1 data) {
    return BlockDataToBlock.parseEth1Data(data);
  }

  public static Fork parseFork(BeaconStateData.Fork data) {
    return new Fork(
        Bytes4.fromHexString(data.getPreviousVersion()),
        Bytes4.fromHexString(data.getCurrentVersion()),
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())));
  }

  public static Crosslink parseCrosslink(CrossLinkData data) {
    return new Crosslink(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        Hash32.fromHexString(data.getPreviousCrosslinkRoot()),
        Hash32.fromHexString(data.getCrosslinkDataRoot()));
  }

  public static PendingAttestation parsePendingAttestation(
      BlockData.AttestationData attestationData) {
    return BlockDataToBlock.parsePendingAttestation(attestationData);
  }

  public static AttestationData parseAttestationData(
      BlockData.AttestationData.AttestationDataContainer data) {
    return BlockDataToBlock.parseAttestationData(data);
  }

  public static Transfer parseTransfer(BlockData.BlockBodyData.TransferData data) {
    return BlockDataToBlock.parseTransfer(data);
  }

  public static VoluntaryExit parseVoluntaryExit(BlockData.BlockBodyData.VoluntaryExitData data) {
    return BlockDataToBlock.parseVoluntaryExit(data);
  }

  public static BeaconBlockHeader parseBeaconBlockHeader(BlockData.BlockHeaderData data) {
    return BlockDataToBlock.parseBeaconBlockHeader(data);
  }

  public static IndexedAttestation parseIndexedAttestation(
      BlockData.BlockBodyData.IndexedAttestationData data) {
    return BlockDataToBlock.parseIndexedAttestation(data);
  }
}
