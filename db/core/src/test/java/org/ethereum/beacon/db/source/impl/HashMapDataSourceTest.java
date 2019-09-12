package org.ethereum.beacon.db.source.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class HashMapDataSourceTest {

    private HashMapDataSource<String, String> dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new HashMapDataSource<>();
        assertThat(dataSource).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({ "test_key, test_value"})
    void testGetPutRemove(String key, String value) {
        assertThat(dataSource.store).doesNotContainKeys(key);
        dataSource.put(key, value);
        assertThat(dataSource.get(key)).isPresent().hasValue(value);
        dataSource.remove(key);
        assertThat(dataSource.get(key)).isNotPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = "test_flush")
    void testFlush(String flush) {
        dataSource.getStore().put(flush, flush);
        assertThat(dataSource.getStore().get(flush)).isEqualTo(flush);
}

    @ParameterizedTest
    @CsvSource({ "test_key, test_value"})
    void testGetStore(String key, String value) {
        dataSource.put(key, value);
        assertThat(dataSource.getStore().get(key)).isEqualTo(value);
        dataSource.getStore().remove(key);
        assertThat(dataSource.get(key)).isNotPresent();
    }



    @ParameterizedTest
    @MethodSource("nullArgumentsProvider")
    void testNullValues(String key, String value) {
        assertThatThrownBy(() -> dataSource.put(key, value)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("not_null", null),
                Arguments.of(null, "not_null")
        );
    }
}