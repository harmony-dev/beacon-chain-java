package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconStateSerializer implements ObjectSerializer<BeaconState> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private ForkSerializer forkSerializer;
  private ValidatorSerializer validatorSerializer;
  private PendingAttestationSerializer pendingAttestationSerializer;
  private CrosslinkSerializer crosslinkSerializer;
  private BeaconBlockHeaderSerializer beaconBlockHeaderSerializer;
  private Eth1DataSerializer eth1DataSerializer;

  public BeaconStateSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.forkSerializer = new ForkSerializer(mapper);
    this.validatorSerializer = new ValidatorSerializer(mapper);
    this.pendingAttestationSerializer = new PendingAttestationSerializer(mapper);
    this.crosslinkSerializer = new CrosslinkSerializer(mapper);
    this.beaconBlockHeaderSerializer = new BeaconBlockHeaderSerializer(mapper);
    this.eth1DataSerializer = new Eth1DataSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return BeaconState.class;
  }

  @Override
  public ObjectNode map(BeaconState instance) {
    ObjectNode beaconState = mapper.createObjectNode();
    ObjectSerializer.setUint64Field(beaconState, "slot", instance.getSlot());
    ObjectSerializer.setUint64Field(beaconState, "genesis_time", instance.getGenesisTime());
    beaconState.set("fork", forkSerializer.map(instance.getFork()));

    ArrayNode validatorRegistryNode = mapper.createArrayNode();
    instance.getValidatorRegistry().stream().map(o -> validatorSerializer.map(o)).forEachOrdered(validatorRegistryNode::add);
    beaconState.set("validator_registry", validatorRegistryNode);
    ArrayNode balancesNode = mapper.createArrayNode();
    instance.getValidatorBalances().stream().map(ObjectSerializer::convert).forEachOrdered(balancesNode::add);
    beaconState.set("balances", balancesNode);

    ArrayNode latestRandaoMixes = mapper.createArrayNode();
    instance.getLatestRandaoMixes().stream()
        .map(Hash32::toString)
        .forEachOrdered(latestRandaoMixes::add);
    beaconState.set("latest_randao_mixes", latestRandaoMixes);
    ObjectSerializer.setUint64Field(beaconState, "latest_start_shard", instance.getCurrentShufflingStartShard());// TODO

    ArrayNode previousEpochAttestationNode = mapper.createArrayNode();
    instance.getPreviousEpochAttestations().stream().map(o -> pendingAttestationSerializer.map(o)).forEachOrdered(previousEpochAttestationNode::add);
    beaconState.set("previous_epoch_attestations", previousEpochAttestationNode);
    ArrayNode currentEpochAttestationsNode = mapper.createArrayNode();
    instance.getCurrentEpochAttestations().stream().map(o -> pendingAttestationSerializer.map(o)).forEachOrdered(currentEpochAttestationsNode::add);
    beaconState.set("current_epoch_attestations", currentEpochAttestationsNode);
    ObjectSerializer.setUint64Field(beaconState, "previous_justified_epoch", instance.getPreviousJustifiedEpoch());
    ObjectSerializer.setUint64Field(beaconState, "current_justified_epoch", instance.getCurrentJustifiedEpoch());
    beaconState.put("previous_justified_root", instance.getPreviousJustifiedRoot().toString());
    beaconState.put("current_justified_root", instance.getCurrentJustifiedRoot().toString());
    beaconState.put("justification_bitfield", instance.getJustificationBitfield().toString());
    ObjectSerializer.setUint64Field(beaconState, "finalized_epoch", instance.getFinalizedEpoch());
    beaconState.put("finalized_root", instance.getFinalizedRoot().toString());

    ArrayNode currentCrosslinksNode = mapper.createArrayNode();
    instance.getCurrentCrosslinks().stream().map(o -> crosslinkSerializer.map(o)).forEachOrdered(currentCrosslinksNode::add);
    beaconState.set("current_crosslinks", currentCrosslinksNode);
    ArrayNode previousCrosslinksNode = mapper.createArrayNode();
    instance.getPreviousCrosslinks().stream().map(o -> crosslinkSerializer.map(o)).forEachOrdered(previousCrosslinksNode::add);
    beaconState.set("previous_crosslinks", previousCrosslinksNode);
    ArrayNode latestBlockRootsNode = mapper.createArrayNode();
    instance.getLatestBlockRoots().stream().map(Hash32::toString).forEachOrdered(latestBlockRootsNode::add);
    beaconState.set("latest_block_roots", latestBlockRootsNode);
    ArrayNode latestStateRootsNode = mapper.createArrayNode();
    instance.getLatestStateRoots().stream().map(Hash32::toString).forEachOrdered(latestStateRootsNode::add);
    beaconState.set("latest_state_roots", latestStateRootsNode);
    ArrayNode latestActiveIndexRootsNode = mapper.createArrayNode();
    instance.getLatestActiveIndexRoots().stream().map(Hash32::toString).forEachOrdered(latestActiveIndexRootsNode::add);
    beaconState.set("latest_active_index_roots", latestActiveIndexRootsNode);
    ArrayNode latestSlashedBalancesNode = mapper.createArrayNode();
    instance.getLatestSlashedBalances().stream()
        .map(ObjectSerializer::convert)
        .forEachOrdered(latestSlashedBalancesNode::add);
    beaconState.set("latest_slashed_balances", latestSlashedBalancesNode);
    beaconState.set("latest_block_header", beaconBlockHeaderSerializer.map(instance.getLatestBlockHeader()));
    ArrayNode historicalRootsNode = mapper.createArrayNode();
    instance.getHistoricalRoots().stream().map(Hash32::toString).forEachOrdered(historicalRootsNode::add);
    beaconState.set("historical_roots", historicalRootsNode);

    beaconState.set("latest_eth1_data", eth1DataSerializer.map(instance.getLatestEth1Data()));
//    ArrayNode eth1DataVotesNode = mapper.createArrayNode(); TODO
//    instance.getEth1DataVotes().stream().map(o -> eth1DataSerializer.map(o)).forEachOrdered(eth1DataVotesNode::add);
//    beaconState.set("eth1_data_votes", eth1DataVotesNode);
    ObjectSerializer.setUint64Field(beaconState, "deposit_index", instance.getDepositIndex());
    return beaconState;
  }
}
