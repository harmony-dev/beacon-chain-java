package org.ethereum.beacon.pow;

import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.List;

/**
 * Merkle Hash Tree <a
 * href="https://en.wikipedia.org/wiki/Merkle_tree">https://en.wikipedia.org/wiki/Merkle_tree</a>
 * with proofs
 *
 * @param <V> Element type
 */
public interface MerkleTree<V> {
  /**
   * Proofs for element with provided index on tree with specified size
   *
   * <p><strong>Note:</strong> result has encoded deposit count value as last element.
   *
   * @param index at index
   * @param size with all tree made of size elements
   * @return proofs
   */
  List<Hash32> getProof(int index, int size);

  /**
   * Root of merkle tree with all elements up to index
   *
   * <p><strong>Note:</strong> computed root includes deposit count by mixing it with tree root.
   *
   * @param index last element index
   * @return tree root
   */
  Hash32 getRoot(int index);

  /**
   * Inserts value in tree / storage
   *
   * @param value Element value
   */
  void addValue(V value);

  /** @return Index of last/highest element */
  int getLastIndex();
}
