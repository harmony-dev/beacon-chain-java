package org.ethereum.beacon.ssz.visitor;

import java.util.Arrays;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class MerkleTrie {
  final BytesValue[] nodes;

  public MerkleTrie(BytesValue[] nodes) {
    this.nodes = nodes;
  }

  public Hash32 getPureRoot() {
    return Hash32.wrap(Bytes32.leftPad(nodes[1]));
  }

  public Hash32 getFinalRoot() {
    return Hash32.wrap(Bytes32.leftPad(nodes[0]));
  }

  public void setFinalRoot(Hash32 mixedInLengthHash) {
    nodes[0] = mixedInLengthHash;
  }

  public MerkleTrie copy() {
    return new MerkleTrie(Arrays.copyOf(nodes, nodes.length));
  }
}
