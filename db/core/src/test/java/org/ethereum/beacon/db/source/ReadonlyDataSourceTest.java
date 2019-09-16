package org.ethereum.beacon.db.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadonlyDataSourceTest {

    private ReadonlyDataSource<String, String> readonlyDataSource;

    @BeforeEach
    void setUp() {
        final HashMap<String, String> store = new HashMap<>();
        readonlyDataSource = key -> {
            Objects.requireNonNull(key);
            return Optional.ofNullable(store.get(key));
        };

        store.put("test_key_0", "test_value_0");
        assertThat(store.get("test_key_0")).isNotNull();
    }

    @Test
    void testGet() {
        assertThat(readonlyDataSource.get("test_key_0")).isPresent().hasValue("test_value_0");
    }

    @Test
    void testInvalidGet() {
        assertThat(readonlyDataSource.get("test_key_1")).isNotPresent();
    }

    @Test
    void testNullValue(){
        assertThatThrownBy(() -> readonlyDataSource.get(null)).isInstanceOf(NullPointerException.class);
    }
}
