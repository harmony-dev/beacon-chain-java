package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.test.type.model.BlockData;

import static org.ethereum.beacon.test.StateTestUtils.parseSlashableAttestation;

public interface AttesterSlashingField extends DataMapperAccessor {
  default AttesterSlashing getAttesterSlashing(SpecConstants constants) {
    final String key = "attester_slashing.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`attester_slashing` not defined");
    }

    try {
      BlockData.BlockBodyData.AttesterSlashingData attesterSlashingData =
          getMapper()
              .readValue(getFiles().get(key), BlockData.BlockBodyData.AttesterSlashingData.class);
      return new AttesterSlashing(
          parseSlashableAttestation(attesterSlashingData.getSlashableAttestation1(), constants),
          parseSlashableAttestation(attesterSlashingData.getSlashableAttestation2(), constants));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
