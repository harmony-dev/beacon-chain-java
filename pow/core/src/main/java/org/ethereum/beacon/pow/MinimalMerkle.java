package org.ethereum.beacon.pow;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static tech.pegasys.artemis.util.bytes.BytesValue.concat;

/**
 * Minimal merkle tree <a
 * href="https://en.wikipedia.org/wiki/Merkle_tree">https://en.wikipedia.org/wiki/Merkle_tree</a>
 * implementation
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

  public Hash32 getZeroHash(int distanceFromBottom) {
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

  // Progressive Merkle
  public List<BytesValue> getZeroHashes() {
    return IntStream.range(0, 32)
        .mapToObj(this::getZeroHash)
        .map(h -> h.slice(0))
        .collect(Collectors.toList());
  }

  // # Add a value to a Merkle tree by using the algo
  // # that stores a branch of sub-roots
  // def add_value(branch, index, value):
  //    i = 0
  //    while (index+1) % 2**(i+1) == 0:
  //        i += 1
  //    for j in range(0, i):
  //        value = hash(branch[j] + value)
  //        # branch[j] = zerohashes[j]
  //    branch[i] = value

  List<BytesValue> add_value(List<BytesValue> branch, int index, BytesValue value) {
    int i = 0;
    while ((index + 1) % (1 << (i + 1)) == 0) {
      ++i;
    }

    BytesValue valueCopy = value;
    for (int j = 0; j < i; ++j) {
      valueCopy = hashFunction.apply(branch.get(j).concat(valueCopy));
    }
    branch.set(i, valueCopy);

    return branch;
  }

  // def get_root_from_branch(branch, size):
  //    r = b'\x00' * 32
  //    for h in range(32):
  //        if (size >> h) % 2 == 1:
  //            r = hash(branch[h] + r)
  //        else:
  //            r = hash(r + zerohashes[h])
  // return r
  BytesValue get_root_from_branch(List<BytesValue> branch, int size) {
    BytesValue r = Bytes32.ZERO.slice(0);
    for (int h = 0; h < treeDepth; ++h) {
      if ((size >> h) % 2 == 1) {
        r = hashFunction.apply(branch.get(h).concat(r));
      } else {
        r = hashFunction.apply(r.concat(getZeroHash(h)));
      }
    }

    return r;
  }
}
