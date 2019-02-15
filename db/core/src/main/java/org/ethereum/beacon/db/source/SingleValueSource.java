package org.ethereum.beacon.db.source;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by Anton Nashatyrev on 19.11.2018.
 */
public interface SingleValueSource<V> {

  Optional<V> get();

  void set(V value);

  void remove();

  static <KeyType, ValueType> SingleValueSource<ValueType> fromDataSource(
      @Nonnull final DataSource<KeyType, ValueType> backDataSource,
      @Nonnull final KeyType valueKey) {
    return fromDataSource(backDataSource, valueKey, Function.identity(), Function.identity());
  }

  static <ValueType, KeyType, SourceValueType> SingleValueSource<ValueType> fromDataSource(
      @Nonnull final DataSource<KeyType, SourceValueType> backDataSource,
      @Nonnull final KeyType valueKey,
      @Nonnull final Function<ValueType, SourceValueType> valueCoder,
      @Nonnull final Function<SourceValueType, ValueType> valueDecoder) {

    final CodecSource.ValueOnly<KeyType, ValueType, SourceValueType> source =
        new CodecSource.ValueOnly<>(backDataSource, valueCoder, valueDecoder);

    return new SingleValueSource<ValueType>() {
      @Override
      public Optional<ValueType> get() {
        return source.get(valueKey);
      }

      @Override
      public void set(final ValueType value) {
        source.put(valueKey, value);
      }

      @Override
      public void remove() {
        source.remove(valueKey);
      }
    };
  }
  static <ValueType> SingleValueSource<ValueType> memSource() {
    return new SingleValueSource<ValueType>() {
      ValueType value;

      @Override
      public Optional<ValueType> get() {
        return Optional.ofNullable(value);
      }

      @Override
      public void set(ValueType value) {
        this.value = value;
      }

      @Override
      public void remove() {
        this.value = null;
      }
    };
  }
}
