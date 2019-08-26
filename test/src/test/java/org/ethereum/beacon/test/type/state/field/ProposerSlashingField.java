package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.test.type.DataMapperAccessor;
import org.ethereum.beacon.test.type.model.BlockData;

import static org.ethereum.beacon.test.StateTestUtils.parseBeaconBlockHeader;

public interface ProposerSlashingField extends DataMapperAccessor {
  default ProposerSlashing getProposerSlashing() {
    final String key = useSszWhenPossible() ? "proposer_slashing.ssz" : "proposer_slashing.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`proposer_slashing` not defined");
    }

    // SSZ
    if (useSszWhenPossible()) {
      return getSszSerializer().decode(getFiles().get(key), ProposerSlashing.class);
    }

    // YAML
    try {
      BlockData.BlockBodyData.ProposerSlashingData proposerSlashingData =
          getMapper()
              .readValue(getFiles().get(key).extractArray(), BlockData.BlockBodyData.ProposerSlashingData.class);
      return new ProposerSlashing(
          ValidatorIndex.of(proposerSlashingData.getProposerIndex()),
          parseBeaconBlockHeader(proposerSlashingData.getHeader1()),
          parseBeaconBlockHeader(proposerSlashingData.getHeader2()));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
