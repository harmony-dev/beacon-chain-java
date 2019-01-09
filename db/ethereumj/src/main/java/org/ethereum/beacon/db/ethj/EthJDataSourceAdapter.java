package org.ethereum.beacon.db.ethj;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.datasource.Source;

import javax.annotation.Nonnull;
import java.util.Optional;


/**
 * Created by Anton Nashatyrev on 27.12.2018.
 */
public class EthJDataSourceAdapter<KeyType, ValueType> implements DataSource<KeyType, ValueType> {
  private final Source<KeyType, ValueType> src;

  public EthJDataSourceAdapter(Source<KeyType, ValueType> src) {
    this.src = src;
  }

  public void put(@Nonnull KeyType keyType,
                  @Nonnull ValueType valueType) {
    src.put(keyType, valueType);
  }

  public Optional<ValueType> get(@Nonnull KeyType keyType) {
    return Optional.ofNullable(src.get(keyType));
  }

  public void remove(@Nonnull KeyType keyType) {
    src.delete(keyType);
  }

  public void flush() {
    src.flush();
  }
}
