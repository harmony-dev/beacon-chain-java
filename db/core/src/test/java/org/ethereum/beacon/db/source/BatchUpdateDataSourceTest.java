package org.ethereum.beacon.db.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchUpdateDataSourceTest {

    private BatchUpdateDataSource<String, String> dataSource;

    @BeforeEach
    void setUp()
    {
        final HashMap<String, String> store = new HashMap<>();
        dataSource = new BatchUpdateDataSource<String, String>() {
            @Override
            public void batchUpdate(Map<String, String> updates) {
                updates.forEach(this::put);
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
            }

            @Override
            public void remove(@Nonnull String key) {
                Objects.requireNonNull(key);
                store.remove(key);
            }

            @Override
            public void flush() {
                store.put("test_flush", "test_flush");
            }
        };

        assertThat(dataSource.get("something")).isNotPresent();
    }

    @Test
    void testBatchUpdate() {
        final Map<String, String> updates = new HashMap<>();
        updates.put("test_key_1", "test_value_1");
        updates.put("test_key_2", "test_value_2");
        updates.put("test_key_3", "test_value_3");
        updates.put("test_key_4", "test_value_4");
        updates.put("test_key_5", "test_value_5");
        dataSource.batchUpdate(updates);
        assertThat(dataSource.get("test_key_1")).isPresent().hasValue("test_value_1");
        assertThat(dataSource.get("test_key_2")).isPresent().hasValue("test_value_2");
        assertThat(dataSource.get("test_key_3")).isPresent().hasValue("test_value_3");
        assertThat(dataSource.get("test_key_4")).isPresent().hasValue("test_value_4");
        assertThat(dataSource.get("test_key_5")).isPresent().hasValue("test_value_5");
    }

    @Test
    void testFlush() {
        dataSource.flush();
        assertThat(dataSource.get("test_flush")).isPresent().hasValue("test_flush");
    }

    @Test
    void testPutGetRemove() {
        assertThat(dataSource.get("test_key_0")).isNotPresent();
        dataSource.put("test_key_0", "test_value_0");
        assertThat(dataSource.get("test_key_0")).isPresent().hasValue("test_value_0");
        dataSource.remove("test_key_0");
        assertThat(dataSource.get("test_key_0")).isNotPresent();
    }

    @Test
    void testNullValues() {
        assertThatThrownBy(() -> dataSource.put(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put("not_null", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put(null, "not_null")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.remove(null)).isInstanceOf(NullPointerException.class);

        final HashMap updates = new HashMap();
        updates.put(null, null);
        updates.put(null, "test_value_0");
        updates.put("test_value_0", null);
        assertThatThrownBy(() -> dataSource.batchUpdate(updates)).isInstanceOf(NullPointerException.class);
    }
}
