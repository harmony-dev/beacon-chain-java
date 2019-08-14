package org.ethereum.beacon.db.source;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * A clue in between {@link DataSource} of any kind and {@link BatchUpdateDataSource}.
 *
 * <p>Accumulates changes in buffer and then flushes them in a batch via {@link
 * BatchUpdateDataSource#batchUpdate(Map)}.
 *
 * <p>Removals are stored as key-value pairs with value equal to {@code null}.
 *
 * @param <KeyType> a key type.
 * @param <ValueType> a value type.
 */
public class BatchWriter<KeyType, ValueType>
    extends AbstractLinkedDataSource<KeyType, ValueType, KeyType, ValueType> {

  private final Map<KeyType, ValueType> buffer = new HashMap<>();

  public BatchWriter(@Nonnull BatchUpdateDataSource<KeyType, ValueType> upstreamSource) {
    super(upstreamSource);
  }

  @Override
  public Optional<ValueType> get(@Nonnull KeyType key) {
    return getUpstream().get(key);
  }

  @Override
  public void put(@Nonnull KeyType key, @Nonnull ValueType value) {
    buffer.put(key, value);
  }

  @Override
  public void remove(@Nonnull KeyType key) {
    buffer.put(key, null);
  }

  @Override
  protected void doFlush() {
    if (!buffer.isEmpty()) {
      ((BatchUpdateDataSource<KeyType, ValueType>) getUpstream()).batchUpdate(buffer);
      buffer.clear();
    }
  }
}
