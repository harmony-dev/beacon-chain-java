package org.ethereum.beacon.pow;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Merkle Tree <a href="https://en.wikipedia.org/wiki/Merkle_tree">https://en.wikipedia.org/wiki/Merkle_tree</a> with proofs
 * @param <V>   Element type
 */
public interface MerkleTree<V> {
  /**
   * Proofs for element
   * @param index at index
   * @param size  with all tree made of size elements
   * @return  proofs
   */
  ReadVector<Integer, Hash32> getProof(int index, int size);

  /**
   * Deposit Root of merkle tree with all elements up to index
   * @param index   last element index
   * @return  deposit root
   */
  Hash32 getDepositRoot(UInt64 index);

  /**
   * Inserts value in tree / storage
   * @param value   Element value
   */
  void insertValue(V value);

  /**
   * @return Index of last/highest element
   */
  int getLastIndex();
}
