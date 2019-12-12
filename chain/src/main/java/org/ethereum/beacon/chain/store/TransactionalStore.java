package org.ethereum.beacon.chain.store;

import org.ethereum.beacon.consensus.spec.ForkChoice.Store;

public interface TransactionalStore extends Store {

  static TransactionalStore inMemoryStore() {
    return new InMemoryStore();
  }

  boolean isInitialized();

  StoreTx newTx();

  interface StoreTx extends Store {
    void commit();
  }
}
