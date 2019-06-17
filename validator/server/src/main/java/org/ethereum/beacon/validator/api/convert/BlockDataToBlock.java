package org.ethereum.beacon.validator.api.convert;

import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Bitfield;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.api.model.BlockData;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BlockDataToBlock {
  public BlockDataToBlock() {}

  public static BlockData serialize(BeaconBlock block) {
    BlockData data = new BlockData();
    data.setPreviousBlockRoot(block.getPreviousBlockRoot().toString());
    data.setSignature(block.getSignature().toHexString());
    data.setSlot(block.getSlot().toStringNumber(null));
    data.setStateRoot(block.getStateRoot().toString());
    BlockData.BlockBodyData bodyData = new BlockData.BlockBodyData();

    // Attestations
    List<BlockData.AttestationData> attestationDataList = new ArrayList<>();
    block.getBody().getAttestations().stream()
        .forEach(
            a -> {
              BlockData.AttestationData attestation = new BlockData.AttestationData();
              attestation.setAggregationBitfield(a.getAggregationBitfield().toString());
              attestation.setCustodyBitfield(a.getCustodyBitfield().toString());
              attestation.setData(presentAttestationData(a.getData()));
              attestation.setSignature(a.getSignature().toHexString());
              attestationDataList.add(attestation);
            });
    bodyData.setAttestations(attestationDataList);

    // Attestation Slashings
    List<BlockData.BlockBodyData.AttesterSlashingData> slashingList = new ArrayList<>();
    block
        .getBody()
        .getAttesterSlashings()
        .forEach(
            s -> {
              BlockData.BlockBodyData.AttesterSlashingData slashingData =
                  new BlockData.BlockBodyData.AttesterSlashingData();
              slashingData.setSlashableAttestation1(presentIndexedAttestation(s.getAttestation1()));
              slashingData.setSlashableAttestation2(presentIndexedAttestation(s.getAttestation2()));
              slashingList.add(slashingData);
            });
    bodyData.setAttesterSlashings(slashingList);

    // Deposits
    List<BlockData.BlockBodyData.DepositData> depositList = new ArrayList<>();
    block
        .getBody()
        .getDeposits()
        .forEach(
            d -> {
              BlockData.BlockBodyData.DepositData deposit =
                  new BlockData.BlockBodyData.DepositData();
              deposit.setIndex(d.getIndex().longValue());
              deposit.setProof(
                  d.getProof().stream().map(Hash32::toString).collect(Collectors.toList()));
              BlockData.BlockBodyData.DepositData.DepositDataContainer depositData =
                  new BlockData.BlockBodyData.DepositData.DepositDataContainer();
              depositData.setAmount(d.getData().getAmount().toString());
              depositData.setPubkey(d.getData().getPubKey().toHexString());
              depositData.setSignature(d.getData().getSignature().toHexString());
              depositData.setWithdrawalCredentials(
                  d.getData().getWithdrawalCredentials().toString());
              deposit.setData(depositData);
              depositList.add(deposit);
            });
    bodyData.setDeposits(depositList);

    // Eth1 data
    BlockData.BlockBodyData.Eth1 eth1 = new BlockData.BlockBodyData.Eth1();
    eth1.setBlockHash(block.getBody().getEth1Data().getBlockHash().toString());
    eth1.setDepositCount(block.getBody().getEth1Data().getDepositCount().toString());
    eth1.setDepositRoot(block.getBody().getEth1Data().getDepositRoot().toString());
    bodyData.setEth1Data(eth1);

    // Proposer slashings
    List<BlockData.BlockBodyData.ProposerSlashingData> proposerSlashingList = new ArrayList<>();
    block.getBody().getProposerSlashings().stream()
        .forEach(
            s -> {
              BlockData.BlockBodyData.ProposerSlashingData slashingData =
                  new BlockData.BlockBodyData.ProposerSlashingData();
              slashingData.setHeader1(presentBlockHeader(s.getHeader1()));
              slashingData.setHeader2(presentBlockHeader(s.getHeader2()));
              slashingData.setProposerIndex(s.getProposerIndex().longValue());
              proposerSlashingList.add(slashingData);
            });
    bodyData.setProposerSlashings(proposerSlashingList);

    // Transfers
    List<BlockData.BlockBodyData.TransferData> transferList = new ArrayList<>();
    block.getBody().getTransfers().stream()
        .forEach(
            t -> {
              BlockData.BlockBodyData.TransferData transferData =
                  new BlockData.BlockBodyData.TransferData();
              transferData.setAmount(t.getAmount().toString());
              transferData.setFee(t.getFee().toString());
              transferData.setPubkey(t.getPubkey().toHexString());
              transferData.setRecipient(t.getRecipient().longValue());
              transferData.setSender(t.getSender().longValue());
              transferData.setSignature(t.getSignature().toHexString());
              transferData.setSlot(t.getSlot().toStringNumber(null));
              transferList.add(transferData);
            });
    bodyData.setTransfers(transferList);

    // Voluntary exits
    List<BlockData.BlockBodyData.VoluntaryExitData> voluntaryExitList = new ArrayList<>();
    block.getBody().getVoluntaryExits().stream()
        .forEach(
            e -> {
              BlockData.BlockBodyData.VoluntaryExitData exitData =
                  new BlockData.BlockBodyData.VoluntaryExitData();
              exitData.setEpoch(e.getEpoch().toString());
              exitData.setSignature(e.getSignature().toHexString());
              exitData.setValidatorIndex(e.getValidatorIndex().longValue());
              voluntaryExitList.add(exitData);
            });
    bodyData.setVoluntaryExits(voluntaryExitList);

    bodyData.setGraffiti(block.getBody().getGraffiti().toString());
    bodyData.setRandaoReveal(block.getBody().getRandaoReveal().toHexString());
    data.setBody(bodyData);
    return data;
  }

  public static BlockData.BlockHeaderData presentBlockHeader(BeaconBlockHeader header) {
    BlockData.BlockHeaderData headerData = new BlockData.BlockHeaderData();
    headerData.setBlockBodyRoot(header.getBlockBodyRoot().toString());
    headerData.setPreviousBlockRoot(header.getPreviousBlockRoot().toString());
    headerData.setSignature(header.getSignature().toHexString());
    headerData.setSlot(header.getSlot().toStringNumber(null));
    headerData.setStateRoot(header.getStateRoot().toString());

    return headerData;
  }

  public static BlockData.BlockBodyData.IndexedAttestationData presentIndexedAttestation(
      IndexedAttestation attestation) {
    BlockData.BlockBodyData.IndexedAttestationData indexedAttestation =
        new BlockData.BlockBodyData.IndexedAttestationData();
    indexedAttestation.setCustodyBit0Indices(
        attestation.getCustodyBit0Indices().stream()
            .map(UInt64::longValue)
            .collect(Collectors.toList()));
    indexedAttestation.setCustodyBit1Indices(
        attestation.getCustodyBit1Indices().stream()
            .map(UInt64::longValue)
            .collect(Collectors.toList()));
    indexedAttestation.setData(presentAttestationData(attestation.getData()));
    indexedAttestation.setSignature(attestation.getSignature().toHexString());

    return indexedAttestation;
  }

  public static BlockData.AttestationData.AttestationDataContainer presentAttestationData(
      AttestationData data) {
    BlockData.AttestationData.AttestationDataContainer attestationData =
        new BlockData.AttestationData.AttestationDataContainer();
    attestationData.setBeaconBlockRoot(data.getBeaconBlockRoot().toString());
    attestationData.setCrosslinkDataRoot(data.getCrosslinkDataRoot().toString());
    attestationData.setPreviousCrosslinkRoot(data.getPreviousCrosslinkRoot().toString());
    attestationData.setShard(data.getShard().longValue());
    attestationData.setSourceEpoch(data.getSourceEpoch().toString());
    attestationData.setSourceRoot(data.getSourceRoot().toString());
    attestationData.setTargetEpoch(data.getTargetEpoch().toString());
    attestationData.setTargetRoot(data.getTargetRoot().toString());

    return attestationData;
  }

  public static BeaconBlock deserialize(BlockData blockData) {
    Eth1Data eth1Data1 = parseEth1Data(blockData.getBody().getEth1Data());

    // Attestations
    List<Attestation> attestations = new ArrayList<>();
    for (BlockData.AttestationData attestationData : blockData.getBody().getAttestations()) {
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
            .map(
                s ->
                    new AttesterSlashing(
                        parseIndexedAttestation(s.getSlashableAttestation1()),
                        parseIndexedAttestation(s.getSlashableAttestation2())))
            .collect(Collectors.toList());

    // Deposits
    List<Deposit> deposits = new ArrayList<>();
    for (BlockData.BlockBodyData.DepositData depositData : blockData.getBody().getDeposits()) {
      Deposit deposit =
          new Deposit(
              ReadVector.wrap(
                  depositData.getProof().stream()
                      .map(Hash32::fromHexString)
                      .collect(Collectors.toList()),
                  Integer::new),
              UInt64.valueOf(depositData.getIndex()),
              new DepositData(
                  BLSPubkey.fromHexString(depositData.getData().getPubkey()),
                  Hash32.fromHexString(depositData.getData().getWithdrawalCredentials()),
                  Gwei.castFrom(UInt64.valueOf(depositData.getData().getAmount())),
                  BLSSignature.wrap(Bytes96.fromHexString(depositData.getData().getSignature()))));
      deposits.add(deposit);
    }

    // Proposer slashings
    List<ProposerSlashing> proposerSlashings = new ArrayList<>();
    for (BlockData.BlockBodyData.ProposerSlashingData proposerSlashingData :
        blockData.getBody().getProposerSlashings()) {
      BeaconBlockHeader header1 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader1().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getPreviousBlockRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader1().getBlockBodyRoot()),
              BLSSignature.wrap(
                  Bytes96.fromHexString(proposerSlashingData.getHeader1().getSignature())));
      BeaconBlockHeader header2 =
          new BeaconBlockHeader(
              SlotNumber.castFrom(UInt64.valueOf(proposerSlashingData.getHeader2().getSlot())),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getPreviousBlockRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getStateRoot()),
              Hash32.fromHexString(proposerSlashingData.getHeader2().getBlockBodyRoot()),
              BLSSignature.wrap(
                  Bytes96.fromHexString(proposerSlashingData.getHeader2().getSignature())));
      ProposerSlashing proposerSlashing =
          new ProposerSlashing(
              ValidatorIndex.of(proposerSlashingData.getProposerIndex()), header1, header2);
      proposerSlashings.add(proposerSlashing);
    }

    // Transfers
    List<Transfer> transfers = new ArrayList<>();
    for (BlockData.BlockBodyData.TransferData transferData : blockData.getBody().getTransfers()) {
      Transfer transfer = parseTransfer(transferData);
      transfers.add(transfer);
    }

    // Voluntary exits
    List<VoluntaryExit> voluntaryExits =
        blockData.getBody().getVoluntaryExits().stream()
            .map(BlockDataToBlock::parseVoluntaryExit)
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
            Hash32.fromHexString(blockData.getPreviousBlockRoot()),
            Hash32.fromHexString(blockData.getStateRoot()),
            blockBody,
            BLSSignature.wrap(Bytes96.fromHexString(blockData.getSignature())));

    return block;
  }

  public static IndexedAttestation parseIndexedAttestation(
      BlockData.BlockBodyData.IndexedAttestationData data) {
    return new IndexedAttestation(
        data.getCustodyBit0Indices().stream().map(ValidatorIndex::of).collect(Collectors.toList()),
        data.getCustodyBit1Indices().stream().map(ValidatorIndex::of).collect(Collectors.toList()),
        parseAttestationData(data.getData()),
        data.getAggregateSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getAggregateSignature()))
            : BLSSignature.ZERO);
  }

  public static Eth1Data parseEth1Data(BlockData.BlockBodyData.Eth1 data) {
    return new Eth1Data(
        Hash32.fromHexString(data.getDepositRoot()),
        UInt64.valueOf(data.getDepositCount()),
        Hash32.fromHexString(data.getBlockHash()));
  }

  public static BeaconBlockHeader parseBeaconBlockHeader(BlockData.BlockHeaderData data) {
    return new BeaconBlockHeader(
        SlotNumber.castFrom(UInt64.valueOf(data.getSlot())),
        Hash32.fromHexString(data.getPreviousBlockRoot()),
        Hash32.fromHexString(data.getStateRoot()),
        Hash32.fromHexString(data.getBlockBodyRoot()),
        data.getSignature() == null
            ? BLSSignature.ZERO
            : BLSSignature.wrap(Bytes96.fromHexString(data.getSignature())));
  }

  public static PendingAttestation parsePendingAttestation(
      BlockData.AttestationData attestationData) {
    return new PendingAttestation(
        Bitfield.of(BytesValue.fromHexString(attestationData.getAggregationBitfield())),
        parseAttestationData(attestationData.getData()),
        SlotNumber.castFrom(UInt64.valueOf(attestationData.getInclusionDelay())),
        ValidatorIndex.of(attestationData.getProposerIndex()));
  }

  public static AttestationData parseAttestationData(
      BlockData.AttestationData.AttestationDataContainer data) {
    return new AttestationData(
        Hash32.fromHexString(data.getBeaconBlockRoot()),
        EpochNumber.castFrom(UInt64.valueOf(data.getSourceEpoch())),
        Hash32.fromHexString(data.getSourceRoot()),
        EpochNumber.castFrom(UInt64.valueOf(data.getTargetEpoch())),
        Hash32.fromHexString(data.getTargetRoot()),
        ShardNumber.of(data.getShard()),
        Hash32.fromHexString(data.getPreviousCrosslinkRoot()),
        Hash32.fromHexString(data.getCrosslinkDataRoot()));
  }

  public static Transfer parseTransfer(BlockData.BlockBodyData.TransferData data) {
    return new Transfer(
        ValidatorIndex.of(data.getSender()),
        ValidatorIndex.of(data.getRecipient()),
        Gwei.castFrom(UInt64.valueOf(data.getAmount())),
        Gwei.castFrom(UInt64.valueOf(data.getFee())),
        SlotNumber.castFrom(UInt64.valueOf(data.getSlot())),
        BLSPubkey.fromHexString(data.getPubkey()),
        BLSSignature.wrap(Bytes96.fromHexString(data.getSignature())));
  }

  public static VoluntaryExit parseVoluntaryExit(BlockData.BlockBodyData.VoluntaryExitData data) {
    return new VoluntaryExit(
        EpochNumber.castFrom(UInt64.valueOf(data.getEpoch())),
        ValidatorIndex.of(data.getValidatorIndex()),
        data.getSignature() != null
            ? BLSSignature.wrap(Bytes96.fromHexString(data.getSignature()))
            : BLSSignature.ZERO);
  }
}
