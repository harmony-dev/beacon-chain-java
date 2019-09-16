package org.ethereum.beacon.db.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class DataSourceTest {

    private DataSource<String, String> dataSource;

    @BeforeEach
    void setUp() {
        final Map<String, String> store = new ConcurrentHashMap<>();
        dataSource = new DataSource<String, String>() {
            @Override
            public Optional<String> get(@Nonnull String key) {
                return Optional.ofNullable(store.get(key));
            }

            @Override
            public void put(@Nonnull String key, @Nonnull String value) {
                store.put(key, value);
            }

            @Override
            public void remove(@Nonnull String key) {
                store.remove(key);
            }

            @Override
            public void flush() {
                store.put("test_flush", "test_flush");
            }
        };
    }

    @Test
    void testGetPutRemoveFlush() {
        assertThat(dataSource.get("test_key_0")).isNotPresent();
        dataSource.put("test_key_0", "test_value_0");
        dataSource.put("test_key_1", "test_value_1");
        assertThat(dataSource.get("test_key_0")).isPresent().hasValue("test_value_0");
        assertThat(dataSource.get("test_key_1")).isPresent().hasValue("test_value_1");
        dataSource.remove("test_key_0");
        assertThat(dataSource.get("test_key_0")).isNotPresent();

        dataSource.flush();
        assertThat(dataSource.get("test_flush")).isPresent().hasValue("test_flush");
    }

    @Test
    void testNullValues() {
        assertThatThrownBy(() -> dataSource.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put("not_null", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put(null, "not_null")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.remove(null)).isInstanceOf(NullPointerException.class);
    }
}