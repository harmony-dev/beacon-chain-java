package org.ethereum.beacon.db.source.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class HashMapDataSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";
    private HashMapDataSource<String, String> dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new HashMapDataSource<>();
        assertThat(dataSource).isNotNull();
    }

    @Test
    void testGetPutRemove() {
        assertThat(dataSource.store).doesNotContainKeys(TEST_KEY);
        dataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(dataSource.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);
        dataSource.remove(TEST_KEY);
        assertThat(dataSource.get(TEST_KEY)).isNotPresent();
    }

    @Test
    void testFlush() {
        dataSource.getStore().put("test_flush", "test_flush");
        assertThat(dataSource.getStore().get("test_flush")).isEqualTo("test_flush");
}

    @Test
    void testGetStore() {
        dataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(dataSource.getStore().get(TEST_KEY)).isEqualTo(TEST_VALUE);
        dataSource.getStore().remove(TEST_KEY);
        assertThat(dataSource.get(TEST_KEY)).isNotPresent();
    }

    @Test
    void testNullValues() {
        assertThatThrownBy(() -> dataSource.put(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put("not_null", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dataSource.put(null, "not_null")).isInstanceOf(NullPointerException.class);
    }
}