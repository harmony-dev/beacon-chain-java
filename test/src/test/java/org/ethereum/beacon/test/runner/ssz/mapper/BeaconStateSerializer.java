package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class BeaconStateSerializer implements ObjectSerializer<BeaconStateImpl> {
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
    return BeaconStateImpl.class;
  }

  @Override
  public ObjectNode map(BeaconStateImpl instance) {
    ObjectNode beaconState = mapper.createObjectNode();
    beaconState.set("slot", ComparableBigIntegerNode.valueOf(instance.getSlot()));
    beaconState.set("genesis_time", ComparableBigIntegerNode.valueOf(instance.getGenesisTime()));
    beaconState.set("fork", forkSerializer.map(instance.getFork()));

    ArrayNode validatorRegistryNode = mapper.createArrayNode();
    instance.getValidators().stream().map(o -> validatorSerializer.map(o)).forEachOrdered(validatorRegistryNode::add);
    beaconState.set("validator_registry", validatorRegistryNode);
    ArrayNode balancesNode = mapper.createArrayNode();
    instance.getBalances().stream().map(ComparableBigIntegerNode::valueOf).forEachOrdered(balancesNode::add);
    beaconState.set("balances", balancesNode);

    ArrayNode latestRandaoMixes = mapper.createArrayNode();
    instance.getRandaoMixes().stream()
        .map(Hash32::toString)
        .forEachOrdered(latestRandaoMixes::add);
    beaconState.set("latest_randao_mixes", latestRandaoMixes);
    beaconState.set("latest_start_shard", ComparableBigIntegerNode.valueOf(instance.getStartShard()));

    ArrayNode previousEpochAttestationNode = mapper.createArrayNode();
    instance.getPreviousEpochAttestations().stream().map(o -> pendingAttestationSerializer.map(o)).forEachOrdered(previousEpochAttestationNode::add);
    beaconState.set("previous_epoch_attestations", previousEpochAttestationNode);
    ArrayNode currentEpochAttestationsNode = mapper.createArrayNode();
    instance.getCurrentEpochAttestations().stream().map(o -> pendingAttestationSerializer.map(o)).forEachOrdered(currentEpochAttestationsNode::add);
    beaconState.set("current_epoch_attestations", currentEpochAttestationsNode);
    beaconState.set("previous_justified_epoch", ComparableBigIntegerNode.valueOf(instance.getPreviousJustifiedCheckpoint().getEpoch()));
    beaconState.set("current_justified_epoch", ComparableBigIntegerNode.valueOf(instance.getCurrentJustifiedCheckpoint().getEpoch()));
    beaconState.put("previous_justified_root", instance.getPreviousJustifiedCheckpoint().getRoot().toString());
    beaconState.put("current_justified_root", instance.getCurrentJustifiedCheckpoint().getRoot().toString());
    beaconState.put("justification_bitfield", instance.getJustificationBits().toString());
    beaconState.set("finalized_epoch", ComparableBigIntegerNode.valueOf(instance.getFinalizedCheckpoint().getEpoch()));
    beaconState.put("finalized_root", instance.getFinalizedCheckpoint().getRoot().toString());

    ArrayNode currentCrosslinksNode = mapper.createArrayNode();
    instance.getCurrentCrosslinks().stream().map(o -> crosslinkSerializer.map(o)).forEachOrdered(currentCrosslinksNode::add);
    beaconState.set("current_crosslinks", currentCrosslinksNode);
    ArrayNode previousCrosslinksNode = mapper.createArrayNode();
    instance.getPreviousCrosslinks().stream().map(o -> crosslinkSerializer.map(o)).forEachOrdered(previousCrosslinksNode::add);
    beaconState.set("previous_crosslinks", previousCrosslinksNode);
    ArrayNode latestBlockRootsNode = mapper.createArrayNode();
    instance.getBlockRoots().stream().map(Hash32::toString).forEachOrdered(latestBlockRootsNode::add);
    beaconState.set("latest_block_roots", latestBlockRootsNode);
    ArrayNode latestStateRootsNode = mapper.createArrayNode();
    instance.getStateRoots().stream().map(Hash32::toString).forEachOrdered(latestStateRootsNode::add);
    beaconState.set("latest_state_roots", latestStateRootsNode);
    ArrayNode latestActiveIndexRootsNode = mapper.createArrayNode();
    instance.getActiveIndexRoots().stream().map(Hash32::toString).forEachOrdered(latestActiveIndexRootsNode::add);
    beaconState.set("latest_active_index_roots", latestActiveIndexRootsNode);
    ArrayNode latestSlashedBalancesNode = mapper.createArrayNode();
    instance.getSlashings().stream()
        .map(ComparableBigIntegerNode::valueOf)
        .forEachOrdered(latestSlashedBalancesNode::add);
    beaconState.set("latest_slashed_balances", latestSlashedBalancesNode);
    beaconState.set("latest_block_header", beaconBlockHeaderSerializer.map(instance.getLatestBlockHeader()));
    ArrayNode historicalRootsNode = mapper.createArrayNode();
    instance.getHistoricalRoots().stream().map(Hash32::toString).forEachOrdered(historicalRootsNode::add);
    beaconState.set("historical_roots", historicalRootsNode);

    beaconState.set("latest_eth1_data", eth1DataSerializer.map(instance.getEth1Data()));
    ArrayNode eth1DataVotesNode = mapper.createArrayNode();
    instance.getEth1DataVotes().stream().map(o -> eth1DataSerializer.map(o)).forEachOrdered(eth1DataVotesNode::add);
    beaconState.set("eth1_data_votes", eth1DataVotesNode);
    beaconState.set("deposit_index", ComparableBigIntegerNode.valueOf(instance.getEth1DepositIndex()));
    return beaconState;
  }
}
