package org.ethereum.beacon.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
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
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.type.state.StateTestCase;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData.AttestationData.AttestationDataContainer;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData.BlockHeaderData;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData.CrossLinkData;
import org.ethereum.beacon.test.type.state.StateTestCase.BeaconStateData.ValidatorData;
import org.ethereum.beacon.test.type.state.StateTestCase.BlockData.BlockBodyData.Eth1;
import org.ethereum.beacon.test.type.state.StateTestCase.BlockData.BlockBodyData.IndexedAttestationData;
import org.ethereum.beacon.test.type.state.StateTestCase.BlockData.BlockBodyData.ProposerSlashingData;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

/** Various utility methods aiding state tests development. */
public abstract class StateTestUtils {
  private StateTestUtils() {}

  public static BeaconBlock parseBlockData(
      StateTestCase.BlockData blockData) {
    Eth1Data eth1Data1 = parseEth1Data(blockData.getBody().getEth1Data());

    // Attestations
    List<Attestation> attestations = new ArrayList<>();
    for (StateTestCase.BeaconStateData.AttestationData attestationData :
        blockData.getBody().getAttestations()) {
      AttestationData attestationData1 = parseAttestationData(attestationData.getData());
      Attestation attestation =
          new Attestation(
              Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
              attestationData1,
              Bitfield.of(BytesValue.fromHexString(attestationData.getCustodyBitfield())),
              BLSSignature.wrap(Bytes96.fromHexString(attestationData.getSignature())));
      attestations.add(attestation);
    }

    // Attestation slashings
    List<AttesterSlashing> attesterSlashings =
        blockData.getBody().getAttesterSlashings().stream()
            .map(s -> new AttesterSlashing(
                parseSlashableAttestation(s.getSlashableAttestation1()),
                parseSlashableAttestation(s.getSlashableAttestation2())))
            .collect(Collectors.toList());

    // Deposits
    List<Deposit> deposits = new ArrayList<>();
    for (StateTestCase.BlockData.BlockBodyData.DepositData depositData :
        blockData.getBody().getDeposits()) {
      Deposit deposit =
          Deposit.create(
              depositData.getProof().stream()
                  .map(Hash32::fromHexString)
                  .collect(Collectors.toList()),
              new DepositData(
                  BLSPubkey.fromHexString(depositData.getData().getPubkey()),
                  Hash32.fromHexString(depositData.getData().getWithdrawalCredentials()),
                  Gwei.castFrom(UInt64.valueOf(depositData.getData().getAmount())),
                  BLSSignature.wrap(
                      Bytes96.fromHexString(depositData.getData().getSignature()))));
      deposits.add(deposit);
    }

    // Proposer slashings
    List<ProposerSlashing> proposerSlashings = new ArrayList<>();
    for (ProposerSlashingData proposerSlashingData :
        blockData.getBody().getProposerSlashings()) {
      BeaconBlockHeader header1 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader1().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getParentRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getBodyRoot()),
              BLSSignature.wrap(Bytes96.fromHexString(proposerSlashingData.getHeader1().getSignature())));
      BeaconBlockHeader header2 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader2().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getParentRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getBodyRoot()),
              BLSSignature.wrap(Bytes96.fromHexString(proposerSlashingData.getHeader2().getSignature())));
      ProposerSlashing proposerSlashing =
          new ProposerSlashing(
              ValidatorIndex.of(proposerSlashingData.getProposerIndex()), header1, header2);
      proposerSlashings.add(proposerSlashing);
    }

    // Transfers
    List<Transfer> transfers = new ArrayList<>();
    for (StateTestCase.BlockData.BlockBodyData.TransferData transferData :
        blockData.getBody().getTransfers()) {
      Transfer transfer = parseTransfer(transferData);
      transfers.add(transfer);
    }

    // Voluntary exits
    List<VoluntaryExit> voluntaryExits =
        blockData.getBody().getVoluntaryExits().stream()
            .map(StateTestUtils::parseVoluntaryExit)
            .collect(Collectors.toList());

    // Finally, creating a block
    BeaconBlockBody blockBody =
        BeaconBlockBody.create(
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getBody().getRandaoReveal())),
            eth1Data1,
            Bytes32.fromHexString(blockData.getBody().getGraffiti()),
            proposerSlashings,
            attesterSlashings,
            attestations,
            deposits,
            voluntaryExits,
            transfers);
    BeaconBlock block =
        new BeaconBlock(
            SlotNumber.castFrom(UInt64.valueOf(blockData.getSlot())),
            Hash32.fromHexString(blockData.getParentRoot()),
            Hash32.fromHexString(blockData.getStateRoot()),
            blockBody,
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getSignature())));

    return block;
  }

  public static IndexedAttestation parseSlashableAttestation(IndexedAttestationData data) {
    return new IndexedAttestation(
        data.getCustodyBit0Indices().stream().map(ValidatorIndex::of).collect(Collectors.toList()),
        data.getCustodyBit1Indices().stream().map(ValidatorIndex::of).collect(Collectors.toList()),
        parseAttestationData(data.getData()),
        data.getAggregateSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getAggregateSignature()))
            : BLSSignature.ZERO);
  }

  public static MutableBeaconState parseBeaconState(SpecConstants specConstants, BeaconStateData data) {
    MutableBeaconState state = BeaconState.getEmpty(specConstants).createMutableCopy();

    state.setSlot(SlotNumber.castFrom(UInt64.valueOf(data.getSlot())));
    state.setGenesisTime(Time.of(data.getGenesisTime()));
    state.setFork(parseFork(data.getFork()));
    state.setPreviousJustifiedCheckpoint(
        new Checkpoint(
            EpochNumber.castFrom(UInt64.valueOf(data.getPreviousJustifiedEpoch())),
            Hash32.fromHexString(data.getPreviousJustifiedRoot())));
    state.setCurrentJustifiedCheckpoint(
        new Checkpoint(
            EpochNumber.castFrom(UInt64.valueOf(data.getCurrentJustifiedEpoch())),
            Hash32.fromHexString(data.getCurrentJustifiedRoot())));
    state.setJustificationBits(new Bitfield64(UInt64.valueOf(data.getJustificationBitfield())));
    state.setFinalizedCheckpoint(
        new Checkpoint(
            EpochNumber.castFrom(UInt64.valueOf(data.getFinalizedEpoch())),
            Hash32.fromHexString(data.getFinalizedRoot())));
    state.setLatestBlockHeader(parseBeaconBlockHeader(data.getLatestBlockHeader()));
    state.setEth1Data(parseEth1Data(data.getLatestEth1Data()));
    state.setEth1DataVotes(WriteList.wrap(data.getEth1DataVotes().stream().map(StateTestUtils::parseEth1Data).collect(Collectors.toList()), Integer::new));
    state.setEth1DepositIndex(UInt64.valueOf(data.getDepositIndex()));

    state.getValidators().replaceAll(parseValidatorRegistry(data.getValidatorRegistry()));
    state.getBalances().replaceAll(parseBalances(data.getBalances()));
    state.getRandaoMixes().setAll(parseHashes(data.getLatestRandaoMixes()));
    state.getPreviousEpochAttestations().replaceAll(
        parsePendingAttestations(data.getPreviousEpochAttestations()));
    state.getCurrentEpochAttestations().replaceAll(
        parsePendingAttestations(data.getCurrentEpochAttestations()));
    state.getCurrentCrosslinks().setAll(parseCrosslinks(data.getCurrentCrosslinks()));
    state.getPreviousCrosslinks().setAll(parseCrosslinks(data.getPreviousCrosslinks()));
    state.getBlockRoots().setAll(parseHashes(data.getLatestBlockRoots()));
    state.getStateRoots().setAll(parseHashes(data.getLatestStateRoots()));
    state.getActiveIndexRoots().setAll(parseHashes(data.getLatestActiveIndexRoots()));
    state.getHistoricalRoots().replaceAll(parseHashes(data.getHistoricalRoots()));
    state.getSlashings().setAll(parseBalances(data.getLatestSlashedBalances()));
    state.setStartShard(ShardNumber.of(UInt64.valueOf(data.getLatestStartShard())));

    return state;
  }

  public static List<Crosslink> parseCrosslinks(List<CrossLinkData> data) {
    return data.stream().map(StateTestUtils::parseCrosslink).collect(Collectors.toList());
  }

  public static List<PendingAttestation> parsePendingAttestations(
      List<BeaconStateData.AttestationData> data) {
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
        Gwei.castFrom(UInt64.valueOf(data.getEffectiveBalance())),
        data.getSlashed(),
        EpochNumber.castFrom(UInt64.valueOf(data.getActivationEligibilityEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getActivationEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getExitEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getWithdrawableEpoch())));
  }

  public static Eth1Data parseEth1Data(Eth1 data) {
    return new Eth1Data(
        Hash32.fromHexString(data.getDepositRoot()),
        UInt64.valueOf(data.getDepositCount()),
        Hash32.fromHexString(data.getBlockHash()));
  }

  public static BeaconBlockHeader parseBeaconBlockHeader(BlockHeaderData data) {
    return new BeaconBlockHeader(
        SlotNumber.castFrom(UInt64.valueOf(data.getSlot())),
        Hash32.fromHexString(data.getParentRoot()),
        Hash32.fromHexString(data.getStateRoot()),
        Hash32.fromHexString(data.getBodyRoot()),
        data.getSignature() == null
            ? BLSSignature.ZERO
            : BLSSignature.wrap(Bytes96.fromHexString(data.getSignature())));
  }

  public static Fork parseFork(BeaconStateData.Fork data) {
    return new Fork(
        Bytes4.fromHexString(data.getPreviousVersion()),
        Bytes4.fromHexString(data.getCurrentVersion()),
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())));
  }

  public static Crosslink parseCrosslink(CrossLinkData data) {
    return new Crosslink(
        ShardNumber.of(data.getShard()),
        Hash32.fromHexString(data.getParentRoot()),
        EpochNumber.castFrom(UInt64.valueOf(data.getStartEpoch())),
        EpochNumber.castFrom(UInt64.valueOf(data.getEndEpoch())),
        Hash32.fromHexString(data.getDataRoot())
    );
  }

  public static PendingAttestation parsePendingAttestation(
      StateTestCase.BeaconStateData.AttestationData attestationData) {
    return new PendingAttestation(
        Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
        parseAttestationData(attestationData.getData()),
        SlotNumber.castFrom(UInt64.valueOf(attestationData.getInclusionDelay())),
        ValidatorIndex.of(attestationData.getProposerIndex()));
  }

  public static AttestationData parseAttestationData(AttestationDataContainer data) {
    return new AttestationData(
        Hash32.fromHexString(data.getBeaconBlockRoot()),
        new Checkpoint(
            EpochNumber.castFrom(UInt64.valueOf(data.getSourceEpoch())),
            Hash32.fromHexString(data.getSourceRoot())),
        new Checkpoint(
            EpochNumber.castFrom(UInt64.valueOf(data.getTargetEpoch())),
            Hash32.fromHexString(data.getTargetRoot())),
        parseCrosslink(data.getCrosslink()));
  }

  public static Transfer parseTransfer(StateTestCase.BlockData.BlockBodyData.TransferData data) {
    return new Transfer(
        ValidatorIndex.of(data.getSender()),
        ValidatorIndex.of(data.getRecipient()),
        Gwei.castFrom(UInt64.valueOf(data.getAmount())),
        Gwei.castFrom(UInt64.valueOf(data.getFee())),
        SlotNumber.castFrom(UInt64.valueOf(data.getSlot())),
        BLSPubkey.fromHexString(data.getPubkey()),
        BLSSignature.wrap(Bytes96.fromHexString(data.getSignature())));
  }

  public static VoluntaryExit parseVoluntaryExit(StateTestCase.BlockData.BlockBodyData.VoluntaryExitData data) {
    return new VoluntaryExit(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        ValidatorIndex.of(data.getValidatorIndex()),
        data.getSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getSignature()))
            : BLSSignature.ZERO);
    }
}
