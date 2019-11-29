package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheDataSourceTest {

    private long size;
    private CacheDataSource<String, String> dataSource;

    @BeforeEach
    void setUp() {
        final Function<String, Long> keyEvaluator = getKeyValueEvaluator();
        final Function<String, Long> valueEvaluator = keyEvaluator;

        final Map<String, String> store = new HashMap<>();
        final HashMapDataSource<String, String> upStream = new HashMapDataSource<>();
        dataSource = new CacheDataSource<String, String>() {
            @Override
            public Optional<Optional<String>> getCacheEntry(@Nonnull String key) {
                Objects.requireNonNull(key);
                String entry = store.get(key);
                return Optional.ofNullable(entry == null ? null : Optional.ofNullable(entry));
            }

            @Override
            public long evaluateSize() {
                return size;
            }

            @Nonnull
            @Override
            public DataSource<String, String> getUpstream() {
                return upStream;
            }

            @Override
            public Optional<String> get(@Nonnull String key) {
                Objects.requireNonNull(key);
                return Optional.ofNullable(store.get(key));
            }

            @Override
            public void put(@Nonnull String key, @Nonnull String value) {
                Objects.requireNonNull(key);
                Objects.requireNonNull(value);
                store.put(key, value);
                size += keyEvaluator.apply(key);
                size += valueEvaluator.apply(value);

            }

            @Override
            public void remove(@Nonnull String key) {
                Objects.requireNonNull(key);
                store.remove(key);
                size -= keyEvaluator.apply(key);
                size -= valueEvaluator.apply(key);
            }

            @Override
            public void flush() {
                store.forEach(upStream::put);
                size = 0;
                store.clear();
            }
        };

        assertThat(dataSource.evaluateSize()).isEqualTo(size);
    }

    private Function<String, Long> getKeyValueEvaluator() {
        return s -> 1L;
    }

    @Test
    void testGetPutRemoveFlushCacheEntrySize(){
        assertThat(dataSource.get("test_key_0")).isNotPresent();
        dataSource.put("test_key_0", "test_value_0");
        assertThat(dataSource.evaluateSize()).isGreaterThan(0);

        dataSource.put("test_key_1", "test_value_1");
        dataSource.put("test_key_2", "test_value_2");
        dataSource.put("test_key_3", "test_value_3");
        assertThat(dataSource.getCacheEntry("test_key_0")).isPresent().hasValue(Optional.ofNullable("test_value_0"));

        dataSource.remove("test_key_0");
        assertThat(dataSource.getCacheEntry("test_key_0")).isNotPresent();
        assertThat(dataSource.getCacheEntry("test_key_1")).isPresent().hasValue(Optional.ofNullable("test_value_1"));

        dataSource.flush();
        assertThat(dataSource.evaluateSize()).isEqualTo(0);
        assertThat(dataSource.get("test_key_1")).isNotPresent();
        assertThat(dataSource.getUpstream().get("test_key_1")).isPresent().hasValue("test_value_1");

    }

    @Test
    void testNullValues() {
        assertThatThrownBy(() -> dataSource.put(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put("not_null", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put(null, "not_null")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.remove(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.getCacheEntry(null)).isInstanceOf(NullPointerException.class);
    }
}
