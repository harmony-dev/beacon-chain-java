package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.HoleyList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HashMapHoleyListTest {

    private HoleyList<String> map;

    @BeforeEach
    void setUp() {
        map = new HashMapHoleyList<>();
        assertThat(map.size()).isEqualTo(0L);
    }

    @ParameterizedTest
    @MethodSource("keyValueArgumentsProvider")
    void testPutSize(Long key, String value, Long size) {
        map.put(key, value);
        assertThat(map.size()).isEqualTo(size);
    }

    private static Stream<Arguments> keyValueArgumentsProvider() {
        return Stream.of(
                Arguments.of(0L, "test_value", 1L),
                Arguments.of(0L, null, 0L)
        );
    }

    @ParameterizedTest
    @MethodSource("keyValueGetArgumentsProvider")
    void testValidGet(Long key, String value) {
        map.put(key, value);
        assertThat(map.get(key)).isPresent().hasValue(value);
    }

    private static Stream<Arguments> keyValueGetArgumentsProvider() {
        return Stream.of(
                Arguments.of(0L, "test_value")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidKeyArgumentsProvider")
    void testGetOverIndex(Long key, String value, Long wrongKey) {
        map.put(key, value);
        assertThat(map.get(wrongKey)).isNotPresent();
    }

    private static Stream<Arguments> invalidKeyArgumentsProvider() {
        return Stream.of(
                Arguments.of(0L, "test_value", 1L),
                Arguments.of(0L, "test_value", -1L)
        );
    }

    @Test
    void testPutSameKey() {
        final long TEST_KEY = 0L;
        final long TEST_KEY_1 = 1L;
        final String TEST_VALUE = "test_value";
        final String TEST_VALUE_NEW = "NewTestValue";

        map.put(TEST_KEY, TEST_VALUE);
        map.put(TEST_KEY_1, TEST_VALUE);
        assertThat(map.size()).isEqualTo(2L);

        map.put(TEST_KEY, TEST_VALUE_NEW);
        assertThat(map.size()).isEqualTo(2L);
        assertThat(map.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE_NEW);
    }
}
