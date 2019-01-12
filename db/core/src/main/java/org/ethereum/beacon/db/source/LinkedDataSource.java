package org.ethereum.beacon.db.source;

import javax.annotation.Nonnull;

/**
 * This DataSource type is backed up by another {@link DataSource}e storage (upstream).
 * Upstream source may have different Key or/and Value type. In this case
 * this source would also act as a converter between those types
 *
 * Upstream source generally acts as data originator on data queries
 * All updates are generally forwarded to the upstream source either
 * immediately or on <code>flush()</code> invocation
 */
public interface LinkedDataSource<KeyType, ValueType, UpKeyType, UpValueType>
    extends DataSource<KeyType, ValueType> {

  /**
   * @return Upstream {@link DataSource}
   */
  @Nonnull
  DataSource<UpKeyType, UpValueType> getUpstream();

  /**
   * Optional method.
   *
   * Normally the whole tree of data sources is created during initialization,
   * but it could be useful sometimes to modify this pipeline after creation.
   *
   * Not every implementation may support 'hot swap', i.e. when <code>setUpstream()</code> is
   * called after data flow is already started.
   *   *
   * @throws IllegalStateException when this source doesn't support 'hot swap' and
   * this source started processing data already
   */
  default void setUpstream(@Nonnull DataSource<UpKeyType, UpValueType> newUpstream) {
    throw new UnsupportedOperationException();
  };
}
