package org.ethereum.beacon.db;

import java.util.function.Function;

import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;

/**
 * In memory database implementation based on {@link HashMapDataSource}.
 */
public class InMemoryDatabase extends XorKeyDatabase {

  public InMemoryDatabase() {
    super(new HashMapDataSource<>(), Hashes::sha256);
  }

  @Override
  public void commit() {}

  @Override
  public void close() {}
}
