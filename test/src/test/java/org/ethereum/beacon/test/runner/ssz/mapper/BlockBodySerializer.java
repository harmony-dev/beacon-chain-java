package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.state.HistoricalBatch;

import java.util.Objects;

public class BlockBodySerializer implements ObjectSerializer<BeaconBlockBody> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private Eth1DataSerializer eth1DataSerializer;
  private ProposerSlashingSerializer proposerSlashingSerializer;
  private AttesterSlashingSerializer attesterSlashingSerializer;
  private AttestationSerializer attestationSerializer;
  private DepositSerializer depositSerializer;
  private VoluntaryExitSerializer voluntaryExitSerializer;
  private TransferSerializer transferSerializer;

  public BlockBodySerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.eth1DataSerializer = new Eth1DataSerializer(mapper);
    this.proposerSlashingSerializer = new ProposerSlashingSerializer(mapper);
    this.attesterSlashingSerializer = new AttesterSlashingSerializer(mapper);
    this.attestationSerializer = new AttestationSerializer(mapper);
    this.depositSerializer = new DepositSerializer(mapper);
    this.voluntaryExitSerializer = new VoluntaryExitSerializer(mapper);
    this.transferSerializer = new TransferSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return BeaconBlockBody.class;
  }

  @Override
  public ObjectNode map(BeaconBlockBody instance) {
    ObjectNode blockBody = mapper.createObjectNode();
    blockBody.put("randao_reveal", instance.getRandaoReveal().toString());
    blockBody.set("eth1_data", eth1DataSerializer.map(instance.getEth1Data()));
    ArrayNode proposerSlashingNode = mapper.createArrayNode();
    instance.getProposerSlashings().stream().map(o -> proposerSlashingSerializer.map(o)).forEachOrdered(proposerSlashingNode::add);
    blockBody.set("proposer_slashings", proposerSlashingNode);
    ArrayNode attesterSlashingNode = mapper.createArrayNode();
    instance.getAttesterSlashings().stream().map(o -> attesterSlashingSerializer.map(o)).forEachOrdered(attesterSlashingNode::add);
    blockBody.set("attester_slashings", attesterSlashingNode);
    ArrayNode attestationsNode = mapper.createArrayNode();
    instance.getAttestations().stream().map(o -> attestationSerializer.map(o)).forEachOrdered(attestationsNode::add);
    blockBody.set("attestations", attestationsNode);
    ArrayNode depositsNode = mapper.createArrayNode();
    instance.getDeposits().stream().map(o -> depositSerializer.map(o)).forEachOrdered(depositsNode::add);
    blockBody.set("deposits", depositsNode);
    ArrayNode voluntaryExitsNode = mapper.createArrayNode();
    instance.getVoluntaryExits().stream().map(o -> voluntaryExitSerializer.map(o)).forEachOrdered(voluntaryExitsNode::add);
    blockBody.set("voluntary_exits", voluntaryExitsNode);
    ArrayNode transfersNode = mapper.createArrayNode();
    instance.getTransfers().stream().map(o -> transferSerializer.map(o)).forEachOrdered(transfersNode::add);
    blockBody.set("transfers", transfersNode);
    return blockBody;
  }
}
