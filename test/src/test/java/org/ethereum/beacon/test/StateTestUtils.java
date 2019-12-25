package org.ethereum.beacon.test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.envelops.SignedBeaconBlock;
import org.ethereum.beacon.core.envelops.SignedBeaconBlockHeader;
import org.ethereum.beacon.core.envelops.SignedVoluntaryExit;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
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
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.type.model.BeaconStateData;
import org.ethereum.beacon.test.type.model.BlockData;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.collections.Bitvector;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.uint.UInt64;

/** Various utility methods aiding state tests development. */
public abstract class StateTestUtils {
  private StateTestUtils() {}

  public static SignedBeaconBlock parseSignedBlockData(BlockData blockData, SpecConstants constants) {
    BeaconBlock block = parseBlockData(blockData, constants);
    return new SignedBeaconBlock(block, BLSSignature.wrap(Bytes96.fromHexString(blockData.getSignature())));
  }

  public static BeaconBlock parseBlockData(BlockData blockData, SpecConstants constants) {
    Eth1Data eth1Data1 = parseEth1Data(blockData.getBody().getEth1Data());

    // Attestations
    List<Attestation> attestations = new ArrayList<>();
    for (BeaconStateData.AttestationData attestationData :
        blockData.getBody().getAttestations()) {
      AttestationData attestationData1 = parseAttestationData(attestationData.getData());
      BytesValue aggValue = BytesValue.fromHexString(attestationData.getAggregationBits());
      Attestation attestation =
          new Attestation(
              Bitlist.of(aggValue, constants.getMaxValidatorsPerCommittee().longValue()),
              attestationData1,
              BLSSignature.wrap(Bytes96.fromHexString(attestationData.getSignature())),
              constants);
      attestations.add(attestation);
    }

    // Attestation slashings
    List<AttesterSlashing> attesterSlashings =
        blockData.getBody().getAttesterSlashings().stream()
            .map(
                s ->
                    new AttesterSlashing(
                        parseSlashableAttestation(s.getSlashableAttestation1(), constants),
                        parseSlashableAttestation(s.getSlashableAttestation2(), constants)))
            .collect(Collectors.toList());

    // Deposits
    List<Deposit> deposits = new ArrayList<>();
    for (BlockData.BlockBodyData.DepositData depositData :
        blockData.getBody().getDeposits()) {
      Deposit deposit = parseDeposit(depositData);
      deposits.add(deposit);
    }

    // Proposer slashings
    List<ProposerSlashing> proposerSlashings = new ArrayList<>();
    for (BlockData.BlockBodyData.ProposerSlashingData proposerSlashingData : blockData.getBody().getProposerSlashings()) {
      BeaconBlockHeader header1 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader1().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getParentRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getBodyRoot()));
      BeaconBlockHeader header2 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader2().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getParentRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getBodyRoot()));
      ProposerSlashing proposerSlashing =
          new ProposerSlashing(
              ValidatorIndex.of(proposerSlashingData.getProposerIndex()),
              new SignedBeaconBlockHeader(header1, BLSSignature.ZERO),
              new SignedBeaconBlockHeader(header2, BLSSignature.ZERO));
      proposerSlashings.add(proposerSlashing);
    }

    // Voluntary exits
    List<SignedVoluntaryExit> voluntaryExits =
        blockData.getBody().getVoluntaryExits().stream()
            .map(StateTestUtils::parseVoluntaryExit)
            .collect(Collectors.toList());

    // Finally, creating a block
    BeaconBlockBody blockBody =
        new BeaconBlockBody(
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getBody().getRandaoReveal())),
            eth1Data1,
            Bytes32.fromHexString(blockData.getBody().getGraffiti()),
            proposerSlashings,
            attesterSlashings,
            attestations,
            deposits,
            voluntaryExits,
            constants);
    return new BeaconBlock(
            SlotNumber.castFrom(UInt64.valueOf(blockData.getSlot())),
            Hash32.fromHexString(blockData.getParentRoot()),
            Hash32.fromHexString(blockData.getStateRoot()),
            blockBody);
  }

  public static IndexedAttestation parseSlashableAttestation(
      BlockData.BlockBodyData.IndexedAttestationData data, SpecConstants specConstants) {
    return new IndexedAttestation(
        data.getAttestingIndices().stream().map(ValidatorIndex::of).collect(Collectors.toList()),
        parseAttestationData(data.getData()),
        data.getAggregateSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getAggregateSignature()))
            : BLSSignature.ZERO,
        specConstants);
  }

  public static MutableBeaconState parseBeaconState(
      SpecConstants specConstants, BeaconStateData data) {
    MutableBeaconState state = BeaconState.getEmpty(specConstants).createMutableCopy();

    state.setSlot(SlotNumber.castFrom(UInt64.valueOf(data.getSlot())));
    state.setGenesisTime(Time.of(data.getGenesisTime()));
    state.setFork(parseFork(data.getFork()));
    state.setPreviousJustifiedCheckpoint(parseCheckpoint(data.getPreviousJustifiedCheckpoint()));
    state.setCurrentJustifiedCheckpoint(parseCheckpoint(data.getCurrentJustifiedCheckpoint()));
    state.setJustificationBits(
        Bitvector.of(
            specConstants.getJustificationBitsLength(),
            BytesValue.fromHexString(data.getJustificationBits())));
    state.setFinalizedCheckpoint(parseCheckpoint(data.getFinalizedCheckpoint()));
    state.setLatestBlockHeader(parseBeaconBlockHeader(data.getLatestBlockHeader()).getMessage());
    state.setEth1Data(parseEth1Data(data.getEth1Data()));
    state.setEth1DataVotes(
        WriteList.wrap(
            data.getEth1DataVotes().stream()
                .map(StateTestUtils::parseEth1Data)
                .collect(Collectors.toList()),
            Integer::new));
    state.setEth1DepositIndex(UInt64.valueOf(data.getEth1DepositIndex()));

    state.getValidators().replaceAll(parseValidatorRegistry(data.getValidators()));
    state.getBalances().replaceAll(parseBalances(data.getBalances()));
    state.getRandaoMixes().setAll(parseHashes(data.getRandaoMixes()));
    state
        .getPreviousEpochAttestations()
        .replaceAll(parsePendingAttestations(data.getPreviousEpochAttestations(), specConstants));
    state
        .getCurrentEpochAttestations()
        .replaceAll(parsePendingAttestations(data.getCurrentEpochAttestations(), specConstants));
    state.getSlashings().setAll(parseBalances(data.getSlashings()));

    state.getBlockRoots().setAll(parseHashes(data.getBlockRoots()));
    state.getStateRoots().setAll(parseHashes(data.getStateRoots()));
    state.getHistoricalRoots().setAll(parseHashes(data.getHistoricalRoots()));

    return state;
  }

  public static List<PendingAttestation> parsePendingAttestations(
      List<BeaconStateData.AttestationData> data, SpecConstants constants) {
    return data.stream().map((BeaconStateData.AttestationData attestationData) -> parsePendingAttestation(attestationData, constants)).collect(Collectors.toList());
  }

  public static List<Hash32> parseHashes(List<String> data) {
    return data.stream().map(Hash32::fromHexString).collect(Collectors.toList());
  }

  public static List<Gwei> parseBalances(List<String> data) {
    return data.stream().map(b -> Gwei.castFrom(UInt64.valueOf(b))).collect(Collectors.toList());
  }

  public static List<ValidatorRecord> parseValidatorRegistry(List<BeaconStateData.ValidatorData> data) {
    return data.stream().map(StateTestUtils::parseValidatorRecord).collect(Collectors.toList());
  }

  public static ValidatorRecord parseValidatorRecord(BeaconStateData.ValidatorData data) {
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

  public static Checkpoint parseCheckpoint(BeaconStateData.CheckpointData data) {
    return new Checkpoint(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        Hash32.fromHexString(data.getRoot()));
  }

  public static Eth1Data parseEth1Data(BlockData.BlockBodyData.Eth1 data) {
    return new Eth1Data(
        Hash32.fromHexString(data.getDepositRoot()),
        UInt64.valueOf(data.getDepositCount()),
        Hash32.fromHexString(data.getBlockHash()));
  }

  public static SignedBeaconBlockHeader parseBeaconBlockHeader(BeaconStateData.BlockHeaderData data) {
    return new SignedBeaconBlockHeader(new BeaconBlockHeader(
        SlotNumber.castFrom(UInt64.valueOf(data.getSlot())),
        Hash32.fromHexString(data.getParentRoot()),
        Hash32.fromHexString(data.getStateRoot()),
        Hash32.fromHexString(data.getBodyRoot())),
        BLSSignature.wrap(Bytes96.fromHexString(data.getSignature())));
  }

  public static Deposit parseDeposit(BlockData.BlockBodyData.DepositData data) {
    return Deposit.create(
        data.getProof().stream()
            .map(Hash32::fromHexString)
            .collect(Collectors.toList()),
        new DepositData(
            BLSPubkey.fromHexString(data.getData().getPubkey()),
            Hash32.fromHexString(data.getData().getWithdrawalCredentials()),
            Gwei.castFrom(UInt64.valueOf(data.getData().getAmount())),
            BLSSignature.wrap(Bytes96.fromHexString(data.getData().getSignature()))));
  }

  public static Fork parseFork(BeaconStateData.Fork data) {
    return new Fork(
        Bytes4.fromHexString(data.getPreviousVersion()),
        Bytes4.fromHexString(data.getCurrentVersion()),
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())));
  }

  public static PendingAttestation parsePendingAttestation(
      BeaconStateData.AttestationData attestationData, SpecConstants constants) {
    BytesValue aggValue = BytesValue.fromHexString(attestationData.getAggregationBits());
    return new PendingAttestation(
        Bitlist.of(aggValue, constants.getMaxValidatorsPerCommittee().getValue()),
        parseAttestationData(attestationData.getData()),
        SlotNumber.castFrom(UInt64.valueOf(attestationData.getInclusionDelay())),
        ValidatorIndex.of(attestationData.getProposerIndex()),
        constants);
  }

  public static AttestationData parseAttestationData(BeaconStateData.AttestationData.AttestationDataContainer data) {
    return new AttestationData(
        SlotNumber.of(data.getSlot()),
        CommitteeIndex.of(data.getIndex()),
        Hash32.fromHexString(data.getBeaconBlockRoot()),
        parseCheckpoint(data.getSource()),
        parseCheckpoint(data.getTarget()));
  }

  public static SignedVoluntaryExit parseVoluntaryExit(
      BlockData.BlockBodyData.VoluntaryExitData data) {
    return new SignedVoluntaryExit(new VoluntaryExit(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        ValidatorIndex.of(data.getValidatorIndex())),
        data.getSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getSignature()))
            : BLSSignature.ZERO);
  }

  @NotNull
  public static Attestation parseAttestation(BeaconStateData.AttestationData data, SpecConstants constants) {
    BytesValue aggValue = BytesValue.fromHexString(data.getAggregationBits());
    return new Attestation(
        Bitlist.of(aggValue, constants.getMaxValidatorsPerCommittee().getValue()),
        parseAttestationData(data.getData()),
        data.getSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getSignature()))
            : BLSSignature.ZERO,
        constants);
  }

  public static BeaconStateData serializeBeaconState(BeaconState state) {
    BeaconStateData data = new BeaconStateData();

    data.setSlot(state.getSlot().toStringNumber(null));
    data.setGenesisTime(state.getGenesisTime().longValue());
    data.setFork(serializeFork(state.getFork()));

    data.setPreviousJustifiedCheckpoint(serializeCheckpoint(state.getPreviousJustifiedCheckpoint()));
    data.setCurrentJustifiedCheckpoint(serializeCheckpoint(state.getCurrentJustifiedCheckpoint()));
    data.setJustificationBits(state.getJustificationBits().toString());
    data.setFinalizedCheckpoint(serializeCheckpoint(state.getFinalizedCheckpoint()));
    data.setLatestBlockHeader(serializeBeaconBlockHeader(state.getLatestBlockHeader()));
    data.setEth1Data(serializeEth1Data(state.getEth1Data()));
    data.setEth1DataVotes(state.getEth1DataVotes().stream().map(StateTestUtils::serializeEth1Data).collect(Collectors.toList()));
    data.setEth1DepositIndex(state.getEth1DepositIndex().getValue());

    data.setValidators(serializeValidatorRegistry(state.getValidators()));
    data.setBalances(serializeBalances(state.getBalances()));
    //data.setRandaoMixes(
    //    state.getRandaoMixes().stream().map(Hash32::toString).collect(Collectors.toList()));
    data.setPreviousEpochAttestations(serializePendingAttestations(state.getPreviousEpochAttestations()));
    data.setCurrentEpochAttestations(serializePendingAttestations(state.getCurrentEpochAttestations()));
    //data.setSlashings(serializeBalances(state.getSlashings()));

    return data;
  }

  public static List<BeaconStateData.ValidatorData> serializeValidatorRegistry(ReadList<?,ValidatorRecord> validators) {
    return validators.stream().map(StateTestUtils::serializeValidatorRecord).collect(Collectors.toList());
  }

  public static BeaconStateData.ValidatorData serializeValidatorRecord(ValidatorRecord record) {
    BeaconStateData.ValidatorData data = new BeaconStateData.ValidatorData();

    data.setPubkey(record.getPubKey().toHexString());
    data.setWithdrawalCredentials(record.getWithdrawalCredentials().toString());
    data.setEffectiveBalance(record.getEffectiveBalance().toString());
    data.setSlashed(record.getSlashed());
    data.setActivationEligibilityEpoch(record.getActivationEligibilityEpoch().toString());
    data.setActivationEpoch(record.getActivationEpoch().toString());
    data.setExitEpoch(record.getExitEpoch().toString());
    data.setWithdrawableEpoch(record.getWithdrawableEpoch().toString());

    return data;
  }

  public static List<BeaconStateData.AttestationData> serializePendingAttestations(
      ReadList<?,PendingAttestation> attestations) {
    return attestations.stream().map(StateTestUtils::serializePendingAttestation).collect(Collectors.toList());
  }

  public static BeaconStateData.AttestationData serializePendingAttestation(
      PendingAttestation attestation) {
    BeaconStateData.AttestationData data = new BeaconStateData.AttestationData();

    data.setAggregationBits(serializeBitlist(attestation.getAggregationBits()));
    data.setData(serializeAttestationData(attestation.getData()));
    data.setInclusionDelay(attestation.getInclusionDelay().toStringNumber(null));
    data.setProposerIndex(attestation.getProposerIndex().getValue());

    return data;
  }

  public static BeaconStateData.AttestationData.AttestationDataContainer serializeAttestationData(AttestationData attestationData) {
    BeaconStateData.AttestationData.AttestationDataContainer dataContainer = new BeaconStateData.AttestationData.AttestationDataContainer();

    dataContainer.setSlot(attestationData.getSlot().getValue());
    dataContainer.setIndex(attestationData.getIndex().getValue());
    dataContainer.setBeaconBlockRoot(attestationData.getBeaconBlockRoot().toString());
    dataContainer.setSource(serializeCheckpoint(attestationData.getSource()));
    dataContainer.setTarget(serializeCheckpoint(attestationData.getTarget()));

    return dataContainer;
  }

  public static List<String> serializeBalances(ReadList<?, Gwei> balances) {
    return balances.stream().map(Gwei::toString).collect(Collectors.toList());
  }

  public static BeaconStateData.Fork serializeFork(Fork fork) {
    BeaconStateData.Fork data = new BeaconStateData.Fork();
    data.setPreviousVersion(fork.getPreviousVersion().toString());
    data.setCurrentVersion(fork.getCurrentVersion().toString());
    data.setEpoch(fork.getEpoch().toString());
    return data;
  }

  public static BeaconStateData.CheckpointData serializeCheckpoint(Checkpoint checkpoint) {
    BeaconStateData.CheckpointData data = new BeaconStateData.CheckpointData();
    data.setEpoch(checkpoint.getEpoch().longValue());
    data.setRoot(checkpoint.getRoot().toString());
    return data;
  }

  public static BlockData.BlockBodyData.Eth1 serializeEth1Data(Eth1Data eth1Data) {
    BlockData.BlockBodyData.Eth1 eth1 = new BlockData.BlockBodyData.Eth1();
    eth1.setBlockHash(eth1Data.getBlockHash().toString());
    eth1.setDepositCount(eth1Data.getDepositCount().toString());
    eth1.setDepositRoot(eth1Data.getDepositRoot().toString());
    return eth1;
  }

  public static BeaconStateData.BlockHeaderData serializeSignedBeaconBlockHeader(SignedBeaconBlockHeader header) {
    BeaconStateData.BlockHeaderData data = serializeBeaconBlockHeader(header.getMessage());
    data.setSignature(header.getSignature().toHexString());
    return data;
  }

  public static BeaconStateData.BlockHeaderData serializeBeaconBlockHeader(BeaconBlockHeader header) {
    BeaconStateData.BlockHeaderData data = new BeaconStateData.BlockHeaderData();
    data.setSlot(header.getSlot().toStringNumber(null));
    data.setParentRoot(header.getParentRoot().toString());
    data.setStateRoot(header.getStateRoot().toString());
    data.setParentRoot(header.getBodyRoot().toString());
    return data;
  }

  public static BlockData serializeSignedBlock(SignedBeaconBlock block) {
    BlockData data = serializeBlock(block.getMessage());
    data.setSignature(block.getSignature().toHexString());
    return data;
  }

  public static BlockData serializeBlock(BeaconBlock block) {
    BlockData.BlockBodyData blockBodyData = new BlockData.BlockBodyData();
    blockBodyData.setRandaoReveal(block.getBody().getRandaoReveal().toHexString());
    blockBodyData.setEth1Data(serializeEth1Data(block.getBody().getEth1Data()));
    blockBodyData.setGraffiti(block.getBody().getGraffiti().toString());
    blockBodyData.setProposerSlashings(
        block.getBody().getProposerSlashings().stream()
            .map(StateTestUtils::serializeProposerSlashing)
            .collect(Collectors.toList()));
    blockBodyData.setAttesterSlashings(
        block.getBody().getAttesterSlashings().stream()
            .map(StateTestUtils::serializeAttesterSlashing)
            .collect(Collectors.toList()));
    blockBodyData.setAttestations(
        block.getBody().getAttestations().stream()
            .map(StateTestUtils::serializeAttestation)
            .collect(Collectors.toList()));
    blockBodyData.setDeposits(
        block.getBody().getDeposits().stream()
            .map(StateTestUtils::serializeDeposit)
            .collect(Collectors.toList()));
    blockBodyData.setVoluntaryExits(
        block.getBody().getVoluntaryExits().stream()
            .map(StateTestUtils::serializeSignedVoluntaryExit)
            .collect(Collectors.toList()));

    BlockData blockData = new BlockData();
    blockData.setSlot(block.getSlot().toStringNumber(null));
    blockData.setParentRoot(block.getParentRoot().toString());
    blockData.setStateRoot(block.getStateRoot().toString());
    blockData.setBody(blockBodyData);

    return blockData;
  }

  @NotNull
  public static BeaconStateData.AttestationData serializeAttestation(Attestation attestation) {
    BeaconStateData.AttestationData data = new BeaconStateData.AttestationData();

    data.setAggregationBits(serializeBitlist(attestation.getAggregationBits()));
    data.setData(serializeAttestationData(attestation.getData()));
    data.setSignature(attestation.getSignature().toHexString());

    return data;
  }

  public static String serializeBitlist(Bitlist bits) {
    List<Integer> bitList = bits.getBits();
    bitList.add(bits.size());
    return Bitlist.of(bits.size() + 1, bitList, Math.max(bits.maxSize(), bits.size() + 1)).copy().toString();
  }

  @NotNull
  public static BlockData.BlockBodyData.AttesterSlashingData serializeAttesterSlashing(AttesterSlashing attesterSlashing) {
    BlockData.BlockBodyData.AttesterSlashingData data = new BlockData.BlockBodyData.AttesterSlashingData();
    data.setSlashableAttestation1(serializeSlashableAttestation(attesterSlashing.getAttestation1()));
    data.setSlashableAttestation2(serializeSlashableAttestation(attesterSlashing.getAttestation2()));
    return data;
  }

  @NotNull
  public static BlockData.BlockBodyData.ProposerSlashingData serializeProposerSlashing(ProposerSlashing proposerSlashing) {
    BeaconStateData.BlockHeaderData blockHeaderData1 = serializeSignedBeaconBlockHeader(proposerSlashing.getSignedHeader1());
    BeaconStateData.BlockHeaderData blockHeaderData2 = serializeSignedBeaconBlockHeader(proposerSlashing.getSignedHeader2());
    BlockData.BlockBodyData.ProposerSlashingData proposerSlashingData = new BlockData.BlockBodyData.ProposerSlashingData();
    proposerSlashingData.setProposerIndex(proposerSlashing.getProposerIndex().getValue());
    proposerSlashingData.setHeader1(blockHeaderData1);
    proposerSlashingData.setHeader2(blockHeaderData2);
    return proposerSlashingData;
  }

  public static BlockData.BlockBodyData.VoluntaryExitData serializeSignedVoluntaryExit(
      SignedVoluntaryExit voluntaryExit) {
    BlockData.BlockBodyData.VoluntaryExitData data = serializeVoluntaryExit(voluntaryExit.getMessage());
    data.setSignature(voluntaryExit.getSignature().toHexString());
    return data;
  }

  public static BlockData.BlockBodyData.VoluntaryExitData serializeVoluntaryExit(
      VoluntaryExit voluntaryExit) {
    BlockData.BlockBodyData.VoluntaryExitData data = new BlockData.BlockBodyData.VoluntaryExitData();

    data.setEpoch(voluntaryExit.getEpoch().toString());
    data.setValidatorIndex(voluntaryExit.getValidatorIndex().getValue());

    return data;
  }

  public static BlockData.BlockBodyData.DepositData serializeDeposit(Deposit deposit) {
    BlockData.BlockBodyData.DepositData data = new BlockData.BlockBodyData.DepositData();

    data.setProof(deposit.getProof().stream().map(Object::toString).collect(Collectors.toList()));
    BlockData.BlockBodyData.DepositData.DepositDataContainer dataContainer = new BlockData.BlockBodyData.DepositData.DepositDataContainer();
    dataContainer.setPubkey(deposit.getData().getPubKey().toHexString());
    dataContainer.setWithdrawalCredentials(deposit.getData().getWithdrawalCredentials().toString());
    dataContainer.setAmount(deposit.getData().getAmount().toString());
    dataContainer.setSignature(deposit.getData().getSignature().toHexString());
    data.setData(dataContainer);

    return data;
  }

  public static BlockData.BlockBodyData.IndexedAttestationData serializeSlashableAttestation(
      IndexedAttestation attestation) {
    BlockData.BlockBodyData.IndexedAttestationData data = new BlockData.BlockBodyData.IndexedAttestationData();

    data.setAttestingIndices(attestation.getAttestingIndices().stream().map(UInt64::getValue).collect(Collectors.toList()));
    data.setData(serializeAttestationData(attestation.getData()));
    data.setAggregateSignature(attestation.getSignature().toHexString());

    return data;
  }

}
