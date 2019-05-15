package org.ethereum.beacon.pow;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static tech.pegasys.artemis.util.bytes.BytesValue.concat;

/**
 * Minimal merkle tree <a
 * href="https://en.wikipedia.org/wiki/Merkle_tree">https://en.wikipedia.org/wiki/Merkle_tree</a>
 * implementation, adoption from python code from <a
 * href="https://github.com/ethereum/research/blob/master/spec_pythonizer/utils/merkle_minimal.py">https://github.com/ethereum/research/blob/master/spec_pythonizer/utils/merkle_minimal.py</a></a>
 */
public class MinimalMerkle {

  private final int treeDepth;
  private final Hash32[] zeroHashes;
  private final Function<BytesValue, Hash32> hashFunction;

  public MinimalMerkle(Function<BytesValue, Hash32> hashFunction, int treeDepth) {
    this.hashFunction = hashFunction;
    this.treeDepth = treeDepth;
    this.zeroHashes = new Hash32[treeDepth];
  }

  private Hash32 getZeroHash(int distanceFromBottom) {
    if (zeroHashes[distanceFromBottom] == null) {
      if (distanceFromBottom == 0) {
        zeroHashes[0] = Hash32.ZERO;
      } else {
        Hash32 lowerZeroHash = getZeroHash(distanceFromBottom - 1);
        zeroHashes[distanceFromBottom] = hashFunction.apply(concat(lowerZeroHash, lowerZeroHash));
      }
    }
    return zeroHashes[distanceFromBottom];
  }

  // # Compute a Merkle root of a right-zerobyte-padded 2**32 sized tree
  // def calc_merkle_tree_from_leaves(values):
  //    values = list(values)
  //    tree = [values[::]]
  //    for h in range(32):
  //        if len(values) % 2 == 1:
  //            values.append(zerohashes[h])
  //        values = [hash(values[i] + values[i+1]) for i in range(0, len(values), 2)]
  //        tree.append(values[::])
  //    return tree
  public List<List<BytesValue>> calc_merkle_tree_from_leaves(List<BytesValue> valueList) {
    List<BytesValue> values = new ArrayList<>(valueList);
    List<List<BytesValue>> tree = new ArrayList<>();
    tree.add(values);
    for (int h = 0; h < treeDepth; ++h) {
      if (values.size() % 2 == 1) {
        values.add(getZeroHash(h));
      }
      List<BytesValue> valuesTemp = new ArrayList<>();
      for (int i = 0; i < values.size(); i += 2) {
        valuesTemp.add(hashFunction.apply(values.get(i).concat(values.get(i + 1))));
      }
      values = valuesTemp;
      tree.add(values);
    }

    return tree;
  }

  // def get_merkle_root(values):
  //    return calc_merkle_tree_from_leaves(values)[-1][0]
  public BytesValue get_merkle_root(List<BytesValue> values) {
    List<List<BytesValue>> tree = calc_merkle_tree_from_leaves(values);
    try {
      return tree.get(tree.size() - 1).get(0);
    } catch (Exception ex) {
      throw ex;
    }
  }

  // def get_merkle_proof(tree, item_index):
  //    proof = []
  //    for i in range(32):
  //        subindex = (item_index//2**i)^1
  //        proof.append(tree[i][subindex] if subindex < len(tree[i]) else zerohashes[i])
  //    return proof
  public List<Hash32> get_merkle_proof(List<List<BytesValue>> tree, int item_index) {
    List<Hash32> proof = new ArrayList<>();
    for (int i = 0; i < treeDepth; ++i) {
      int subIndex = (item_index / (1 << i)) ^ 1;
      if (subIndex < tree.get(i).size()) {
        proof.add(Hash32.wrap(Bytes32.leftPad(tree.get(i).get(subIndex))));
      } else {
        proof.add(getZeroHash(i));
      }
    }

    return proof;
  }
}
