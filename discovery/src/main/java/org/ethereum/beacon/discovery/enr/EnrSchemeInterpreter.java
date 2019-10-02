package org.ethereum.beacon.discovery.enr;

import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes32;

public interface EnrSchemeInterpreter {
  /** Returns supported scheme */
  EnrScheme getScheme();

  /** Verifies that `nodeRecord` is of scheme implementation */
  default boolean verify(NodeRecord nodeRecord) {
    return nodeRecord.getIdentityScheme().equals(getScheme());
  }

  /** Delivers nodeId according to identity scheme scheme */
  Bytes32 getNodeId(NodeRecord nodeRecord);

  Object decode(String key, RlpString rlpString);

  RlpString encode(String key, Object object);
}
