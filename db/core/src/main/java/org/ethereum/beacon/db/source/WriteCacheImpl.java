package org.ethereum.beacon.db.source;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * This is stub implementation
 *
 * Created by Anton Nashatyrev on 12.11.2018.
 */
public class WriteCacheImpl<K, V> extends AbstractLinkedDataSource<K,V,K,V>
    implements CacheDataSource<K, V> {

  public WriteCacheImpl(final DataSource<K, V> upstreamSource) {
    super(upstreamSource);
  }

  @Override
  public Optional<V> get(@Nonnull final K key) {
    return Optional.empty();
  }

  @Override
  public void put(@Nonnull final K key, @Nonnull final V value) {

  }

  @Override
  public void remove(@Nonnull final K key) {

  }

  @Override
  public void doFlush() {

  }

  /**
   * Discards all changes accumulated
   */
  public void reset() {

  }

  @Override
  public Optional<Optional<V>> getCacheEntry(@Nonnull final K key) {
    return Optional.empty();
  }
}
