package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;

public class InMemoryDatabase extends PrefixedKeyDatabase {

  public InMemoryDatabase() {
    super(new HashMapDataSource<>());
  }

  @Override
  public void commit() {}

  @Override
  public void close() {}
}
