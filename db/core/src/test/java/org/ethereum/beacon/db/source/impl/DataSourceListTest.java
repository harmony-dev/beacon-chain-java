package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class DataSourceListTest {

    private DataSourceList<String> dataSourceList;

    @BeforeEach
    void setUp() {
        dataSourceList = new DataSourceList<>(new HashMapDataSource<>(), serialize(), deserialize());
        assertThat(dataSourceList).isNotNull();
        assertThat(dataSourceList.size()).isEqualTo(0);
    }

    private Function<String, BytesValue> serialize() {
        return key -> BytesValue.EMPTY;
    }

    private Function<BytesValue, String> deserialize() {
        return key -> "";
    }

    @ParameterizedTest
    @MethodSource("nullArgumentsProvider")
    void testInvalidSourceCreation(DataSource ds, Function f1, Function f2) {
        assertThatThrownBy(() -> new DataSourceList<>(ds, f1, f2))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(
                Arguments.of(new HashMapDataSource<>(), null, Function.identity()),
                Arguments.of(new HashMapDataSource<>(), Function.identity(), null)
        );
    }

    @ParameterizedTest
    @MethodSource("putGetSizeArgumentsProvider")
    void testPutGetSizeSerialization(Long key, String value, int expectedSize) {
        assertThat(dataSourceList.get(key)).isNotPresent();
        dataSourceList.put(key, value);
        assertThat(dataSourceList.size()).isEqualTo(expectedSize);
    }

    private static Stream<Arguments> putGetSizeArgumentsProvider() {
        return Stream.of(
                Arguments.of(0L, "test_value", 1),
                Arguments.of(0L, null, 0),
                Arguments.of(1L, "test_value", 2)
        );
    }

    @Test
    void testGetOverIndex() {
        final Long TEST_KEY_0 = 0L;
        final Long TEST_KEY_1 = 1L;
        final Long TEST_KEY_LESS_ZERO = -1L;
        final String TEST_VALUE = "test_value";

        dataSourceList.put(TEST_KEY_0, TEST_VALUE);
        assertThat(dataSourceList.get(TEST_KEY_LESS_ZERO)).isEmpty();
        assertThat(dataSourceList.get(TEST_KEY_1)).isEmpty();
    }
}
