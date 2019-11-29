package org.ethereum.beacon.db.source.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

class DelegateDataSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";
    private final String TEST_FLUSH = "flush";

    private DelegateDataSource<String, String> delegateDataSource;

    @BeforeEach
    void setUp() {
        delegateDataSource = new DelegateDataSource<>(new HashMapDataSource<>());
        assertThat(delegateDataSource.get(TEST_KEY)).isNotPresent();
    }

    @Test
    void testPutGetRemove() {
        delegateDataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(delegateDataSource.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);

        delegateDataSource.remove(TEST_KEY);
        assertThat(delegateDataSource.get(TEST_KEY)).isNotPresent();
    }

    @ParameterizedTest
    @MethodSource("nullArgumentsProvider")
    void testInvalidSourceCreation(String key, String value) {
        assertThatThrownBy(() -> delegateDataSource.put(key, value)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, "test_value"),
                Arguments.of("test_key", null)
        );
    }

    @Test
    void testFlush() {
        delegateDataSource = new DelegateDataSource<>(new HashMapDataSource<String, String>() {
            public void flush() {
                store.put(TEST_FLUSH, TEST_FLUSH);
            }
        });
        assertThat(delegateDataSource.get(TEST_KEY)).isNotPresent();

        delegateDataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(delegateDataSource.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);

        assertThat(delegateDataSource.get(TEST_FLUSH)).isNotPresent();
        delegateDataSource.flush();
        assertThat(delegateDataSource.get(TEST_FLUSH)).isPresent().hasValue(TEST_FLUSH);
    }
}
