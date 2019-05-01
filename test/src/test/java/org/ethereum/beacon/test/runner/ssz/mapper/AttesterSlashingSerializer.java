package org.ethereum.beacon.test.runner.ssz.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;

public class AttesterSlashingSerializer implements ObjectSerializer<AttesterSlashing> {
  private com.fasterxml.jackson.databind.ObjectMapper mapper;
  private IndexedAttestationSerializer indexedAttestationSerializer;

  public AttesterSlashingSerializer(com.fasterxml.jackson.databind.ObjectMapper mapper) {
    this.mapper = mapper;
    this.indexedAttestationSerializer = new IndexedAttestationSerializer(mapper);
  }

  @Override
  public Class accepts() {
    return AttesterSlashing.class;
  }

  @Override
  public ObjectNode map(AttesterSlashing instance) {
    ObjectNode attesterSlashing = mapper.createObjectNode();
    attesterSlashing.set("attestation_1", indexedAttestationSerializer.map(instance.getAttestation1()));
    attesterSlashing.set("attestation_2", indexedAttestationSerializer.map(instance.getAttestation2()));
    return attesterSlashing;
  }
}
