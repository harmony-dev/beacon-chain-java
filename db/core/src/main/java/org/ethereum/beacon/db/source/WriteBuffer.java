package org.ethereum.beacon.db.source;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ethereum.beacon.db.util.AutoCloseableLock;

/**
 * Accumulates changes made to underlying data source and flushes them upon a {@link #flush()} call.
 *
 * <p>This implementation is thread-safe and featured with cache size evaluator.
 *
 * <p>Created by Anton Nashatyrev on 12.11.2018.
 */
public class WriteBuffer<K, V> extends AbstractLinkedDataSource<K, V, K, V>
    implements CacheDataSource<K, V> {

  /** A buffer. */
  private final Map<K, CacheEntry<V>> buffer = new HashMap<>();

  /** A writer to the upstream. */
  private final UpstreamWriter upstreamWriter;

  /** A size evaluator. */
  private final CacheSizeEvaluator<K, V> sizeEvaluator;

  /** CRUD locks. */
  private final ReadWriteUpdateLock rwuLock = new ReentrantReadWriteUpdateLock();

  private final AutoCloseableLock readLock = AutoCloseableLock.wrap(rwuLock.readLock());
  private final AutoCloseableLock writeLock = AutoCloseableLock.wrap(rwuLock.writeLock());
  private final AutoCloseableLock updateLock = AutoCloseableLock.wrap(rwuLock.updateLock());

  public WriteBuffer(
      @Nonnull final DataSource<K, V> upstreamSource,
      @Nonnull final CacheSizeEvaluator<K, V> sizeEvaluator,
      final boolean upstreamFlush) {
    super(upstreamSource, upstreamFlush);
    Objects.requireNonNull(upstreamSource);
    Objects.requireNonNull(sizeEvaluator);

    this.sizeEvaluator = sizeEvaluator;
    this.upstreamWriter = createUpstreamWriter();
  }

  public WriteBuffer(@Nonnull final DataSource<K, V> upstreamSource, final boolean upstreamFlush) {
    this(upstreamSource, CacheSizeEvaluator.noSizeEvaluator(), upstreamFlush);
  }

  @Override
  public Optional<V> get(@Nonnull final K key) {
    Objects.requireNonNull(key);
    try (AutoCloseableLock l = readLock.lock()) {
      CacheEntry<V> entry = buffer.get(key);
      if (entry == null) {
        return getUpstream().get(key);
      } else if (entry == CacheEntry.REMOVED) {
        return Optional.empty();
      } else {
        return Optional.of(entry.value);
      }
    }
  }

  @Override
  public void put(@Nonnull final K key, @Nonnull final V value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    try (AutoCloseableLock l = writeLock.lock()) {
      buffer.put(key, CacheEntry.of(value));
      sizeEvaluator.added(key, value);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void remove(@Nonnull final K key) {
    Objects.requireNonNull(key);
    try (AutoCloseableLock l = writeLock.lock()) {
      CacheEntry<V> entry = buffer.put(key, CacheEntry.REMOVED);
      if (entry != null && entry.getValue().isPresent()) {
        sizeEvaluator.removed(key, entry.getValue().get());
      }
    }
  }

  @Override
  public void doFlush() {
    try (AutoCloseableLock rl = updateLock.lock()) {
      upstreamWriter.write();
      reset();
    }
  }

  /** Discards all changes accumulated */
  public void reset() {
    try (AutoCloseableLock l = writeLock.lock()) {
      buffer.clear();
      sizeEvaluator.reset();
    }
  }

  @Override
  public Optional<Optional<V>> getCacheEntry(@Nonnull final K key) {
    Objects.requireNonNull(key);

    try (AutoCloseableLock l = readLock.lock()) {
      CacheEntry<V> entry = buffer.get(key);
      return Optional.ofNullable(entry == null ? null : entry.getValue());
    }
  }

  @Override
  public long evaluateSize() {
    return sizeEvaluator.getEvaluatedSize();
  }

  /**
   * A structure holding cache entry.
   *
   * @param <V> a value type.
   */
  private static final class CacheEntry<V> {

    /** Indicates removed value. */
    private static final CacheEntry REMOVED = CacheEntry.of(null);

    private V value;

    private CacheEntry(@Nullable V value) {
      this.value = value;
    }

    private static <V> CacheEntry<V> of(@Nullable V value) {
      return new CacheEntry<>(value);
    }

    private Optional<V> getValue() {
      return Optional.ofNullable(value);
    }
  }

  private UpstreamWriter createUpstreamWriter() {
    if (getUpstream() instanceof BatchUpdateDataSource) {
      return new BatchWriter((BatchUpdateDataSource<K, V>) getUpstream());
    } else {
      return new StreamWriter(getUpstream());
    }
  }

  /**
   * An upstream writer interface.
   *
   * <p>There are two kind of implementation:
   *
   * <ul>
   *   <li>One to work with general {@link DataSource} classes.
   *   <li>One specific to {@link BatchUpdateDataSource} sources.
   * </ul>
   *
   * <p><strong>Note:</strong> Instantiated by {@link #createUpstreamWriter()} depending on the type
   * of upstream source.
   */
  private interface UpstreamWriter {
    void write();
  }

  private final class StreamWriter implements UpstreamWriter {

    private final DataSource<K, V> upstream;

    private StreamWriter(DataSource<K, V> upstream) {
      this.upstream = upstream;
    }

    @Override
    public void write() {
      buffer.forEach((key, value) -> upstream.put(key, value.value));
    }
  }

  private final class BatchWriter implements UpstreamWriter {

    private final BatchUpdateDataSource<K, V> upstream;

    private BatchWriter(BatchUpdateDataSource<K, V> upstream) {
      this.upstream = upstream;
    }

    @Override
    public void write() {
      Map<K, V> updates = new HashMap<>();
      buffer.forEach((key, value) -> updates.put(key, value.value));
      upstream.batchUpdate(updates);
    }
  }
}
