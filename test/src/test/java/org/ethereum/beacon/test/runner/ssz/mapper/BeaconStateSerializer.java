package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.Bitvector;

public class BeaconStateSerializer implements ObjectSerializer<BeaconStateImpl> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private ForkSerializer forkSerializer;
  private ValidatorSerializer validatorSerializer;
  private PendingAttestationSerializer pendingAttestationSerializer;
  private BeaconBlockHeaderSerializer beaconBlockHeaderSerializer;
  private Eth1DataSerializer eth1DataSerializer;
  private CheckpointSerializer checkpointSerializer;
  private SSZSerializer sszSerializer = new SSZBuilder().buildSerializer();

  public BeaconStateSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.forkSerializer = new ForkSerializer(mapper);
    this.validatorSerializer = new ValidatorSerializer(mapper);
    this.pendingAttestationSerializer = new PendingAttestationSerializer(mapper);
    this.beaconBlockHeaderSerializer = new BeaconBlockHeaderSerializer(mapper);
    this.eth1DataSerializer = new Eth1DataSerializer(mapper);
    this.checkpointSerializer = new CheckpointSerializer(mapper);
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
    beaconState.set("validators", validatorRegistryNode);
    ArrayNode balancesNode = mapper.createArrayNode();
    instance.getBalances().stream().map(ComparableBigIntegerNode::valueOf).forEachOrdered(balancesNode::add);
    beaconState.set("balances", balancesNode);

    ArrayNode latestRandaoMixes = mapper.createArrayNode();
    instance.getRandaoMixes().stream()
        .map(Hash32::toString)
        .forEachOrdered(latestRandaoMixes::add);
    beaconState.set("randao_mixes", latestRandaoMixes);

    ArrayNode previousEpochAttestationNode = mapper.createArrayNode();
    instance.getPreviousEpochAttestations().stream().map(o -> pendingAttestationSerializer.map(o)).forEachOrdered(previousEpochAttestationNode::add);
    beaconState.set("previous_epoch_attestations", previousEpochAttestationNode);
    ArrayNode currentEpochAttestationsNode = mapper.createArrayNode();
    instance.getCurrentEpochAttestations().stream().map(o -> pendingAttestationSerializer.map(o)).forEachOrdered(currentEpochAttestationsNode::add);
    beaconState.set("current_epoch_attestations", currentEpochAttestationsNode);
    beaconState.set("current_justified_checkpoint", checkpointSerializer.map(instance.getCurrentJustifiedCheckpoint()));
    beaconState.set("previous_justified_checkpoint", checkpointSerializer.map(instance.getPreviousJustifiedCheckpoint()));
    beaconState.put("justification_bits", sszSerializer.encode2(instance.getJustificationBits()).toString());
    beaconState.set("finalized_checkpoint", checkpointSerializer.map(instance.getFinalizedCheckpoint()));

    ArrayNode latestBlockRootsNode = mapper.createArrayNode();
    instance.getBlockRoots().stream().map(Hash32::toString).forEachOrdered(latestBlockRootsNode::add);
    beaconState.set("block_roots", latestBlockRootsNode);
    ArrayNode latestStateRootsNode = mapper.createArrayNode();
    instance.getStateRoots().stream().map(Hash32::toString).forEachOrdered(latestStateRootsNode::add);
    beaconState.set("state_roots", latestStateRootsNode);
    ArrayNode latestSlashedBalancesNode = mapper.createArrayNode();
    instance.getSlashings().stream()
        .map(ComparableBigIntegerNode::valueOf)
        .forEachOrdered(latestSlashedBalancesNode::add);
    beaconState.set("slashings", latestSlashedBalancesNode);
    beaconState.set("latest_block_header", beaconBlockHeaderSerializer.map(instance.getLatestBlockHeader()));
    ArrayNode historicalRootsNode = mapper.createArrayNode();
    instance.getHistoricalRoots().stream().map(Hash32::toString).forEachOrdered(historicalRootsNode::add);
    beaconState.set("historical_roots", historicalRootsNode);

    beaconState.set("eth1_data", eth1DataSerializer.map(instance.getEth1Data()));
    ArrayNode eth1DataVotesNode = mapper.createArrayNode();
    instance.getEth1DataVotes().stream().map(o -> eth1DataSerializer.map(o)).forEachOrdered(eth1DataVotesNode::add);
    beaconState.set("eth1_data_votes", eth1DataVotesNode);
    beaconState.set("eth1_deposit_index", ComparableBigIntegerNode.valueOf(instance.getEth1DepositIndex()));
    return beaconState;
  }
}
