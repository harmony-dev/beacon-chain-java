package org.ethereum.beacon.chain.storage;

import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import tech.pegasys.artemis.ethereum.core.Hash;

public abstract class AbstractHashKeyStorage<H extends Hash, T> implements HashKeyStorage<H, T> {

  private final ObjectHasher<H> objectHasher;

  public AbstractHashKeyStorage(ObjectHasher<H> objectHasher) {
    this.objectHasher = objectHasher;
  }

  @Override
  public void put(T item) {
    this.put(objectHasher.getHash(item), item);
  }

  protected H hash(T item) {
    return objectHasher.getHash(item);
  }
}
