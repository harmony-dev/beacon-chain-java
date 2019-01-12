package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.DataSource;

import javax.annotation.Nonnull;
import java.util.Optional;

public class DelegateDataSource<KeyType, ValueType> implements DataSource<KeyType, ValueType> {
  private final DataSource<KeyType, ValueType> delegate;

  public DelegateDataSource(DataSource<KeyType, ValueType> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Optional<ValueType> get(@Nonnull KeyType key) {
    return delegate.get(key);
  }

  @Override
  public void put(@Nonnull KeyType key, @Nonnull ValueType value) {
    delegate.put(key, value);
  }

  @Override
  public void remove(@Nonnull KeyType key) {
    delegate.remove(key);
  }

  @Override
  public void flush() {
    delegate.flush();
  }
}
