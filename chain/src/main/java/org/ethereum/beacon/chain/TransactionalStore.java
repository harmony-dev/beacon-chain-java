package org.ethereum.beacon.chain;

import org.ethereum.beacon.chain.store.InMemoryStore;
import org.ethereum.beacon.consensus.spec.ForkChoice.Store;

public interface TransactionalStore extends Store {

  static TransactionalStore inMemoryStore() {
    return new InMemoryStore();
  }

  StoreTx newTx();

  interface StoreTx extends Store {
    void commit();
  }
}
