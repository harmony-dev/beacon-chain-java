package org.ethereum.beacon.chain.storage;

import java.util.function.Function;
import org.ethereum.beacon.core.Hashable;
import tech.pegasys.artemis.ethereum.core.Hash;

public abstract class AbstractHashKeyStorage<H extends Hash, T extends Hashable<H>>
    implements HashKeyStorage<H, T> {

  private final Function<Object, H> hashFunction;

  public AbstractHashKeyStorage(Function<Object, H> hashFunction) {
    this.hashFunction = hashFunction;
  }

  @Override
  public void put(T item) {
    this.put(hash(item), item);
  }

  protected H hash(T item) {
    return hashFunction.apply(item);
  }
}
